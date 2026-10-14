package com.github.theholywaffle.teamspeak3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The RFC 8259 subset Web Query exchanges: strict parsing into typed values and string quoting.
 * Values describe themselves, so callers ask for the shape they expect instead of testing types.
 */
final class WebQueryJson {

    private static final int MAX_DEPTH = 32;

    private WebQueryJson() {}

    /** A parsed JSON value; each accessor fails with {@link IllegalArgumentException} for other shapes. */
    sealed interface Value {

        default Map<String, Value> members(String what) {
            throw new IllegalArgumentException(what + " is not a JSON object");
        }

        default List<Value> elements(String what) {
            throw new IllegalArgumentException(what + " is not a JSON array");
        }

        /** @return the ServerQuery text of a scalar: strings verbatim, numbers as written, booleans as 1/0 */
        default String text(String what) {
            throw new IllegalArgumentException(what + " is not a JSON scalar");
        }
    }

    record JsonObject(Map<String, Value> members) implements Value {
        @Override
        public Map<String, Value> members(String what) {
            return members;
        }
    }

    record JsonArray(List<Value> elements) implements Value {
        @Override
        public List<Value> elements(String what) {
            return elements;
        }
    }

    record JsonString(String value) implements Value {
        @Override
        public String text(String what) {
            return value;
        }
    }

    record JsonNumber(String literal) implements Value {
        @Override
        public String text(String what) {
            return literal;
        }
    }

    record JsonBoolean(boolean value) implements Value {
        @Override
        public String text(String what) {
            return value ? "1" : "0";
        }
    }

    enum JsonNull implements Value {
        INSTANCE;

        @Override
        public String text(String what) {
            return "";
        }
    }

    /**
     * @param json complete JSON text
     * @return the single top-level value
     * @throws IllegalArgumentException if the text is not valid JSON
     */
    static Value parse(String json) {
        var parser = new Parser(json);
        parser.skipWhitespace();
        Value value = parser.value(0);
        parser.skipWhitespace();
        if (parser.position != json.length()) throw parser.error("trailing content");
        return value;
    }

    /** Appends {@code value} as a JSON string literal. */
    static StringBuilder quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20 || (Character.isSurrogate(c) && !validSurrogateAt(value, i))) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"');
    }

    private static boolean validSurrogateAt(String value, int index) {
        char c = value.charAt(index);
        if (Character.isHighSurrogate(c)) {
            return index + 1 < value.length() && Character.isLowSurrogate(value.charAt(index + 1));
        }
        return index > 0 && Character.isHighSurrogate(value.charAt(index - 1));
    }

    private static final class Parser {
        private final String json;
        private int position;

        private Parser(String json) {
            this.json = json;
        }

        private Value value(int depth) {
            if (depth > MAX_DEPTH) throw error("nesting deeper than " + MAX_DEPTH);
            if (position >= json.length()) throw error("unexpected end");
            char c = json.charAt(position);
            return switch (c) {
                case '{' -> object(depth);
                case '[' -> array(depth);
                case '"' -> new JsonString(string());
                case 't' -> literal("true", new JsonBoolean(true));
                case 'f' -> literal("false", new JsonBoolean(false));
                case 'n' -> literal("null", JsonNull.INSTANCE);
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield number();
                    throw error("unexpected character");
                }
            };
        }

        private Value object(int depth) {
            position++;
            Map<String, Value> members = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) return new JsonObject(Collections.unmodifiableMap(members));
            do {
                skipWhitespace();
                if (position >= json.length() || json.charAt(position) != '"') throw error("expected member name");
                String name = string();
                skipWhitespace();
                if (!consume(':')) throw error("expected ':'");
                skipWhitespace();
                members.put(name, value(depth + 1));
                skipWhitespace();
            } while (consume(','));
            if (!consume('}')) throw error("expected ',' or '}'");
            return new JsonObject(Collections.unmodifiableMap(members));
        }

        private Value array(int depth) {
            position++;
            List<Value> elements = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) return new JsonArray(List.of());
            do {
                skipWhitespace();
                elements.add(value(depth + 1));
                skipWhitespace();
            } while (consume(','));
            if (!consume(']')) throw error("expected ',' or ']'");
            return new JsonArray(List.copyOf(elements));
        }

        private String string() {
            position++;
            var out = new StringBuilder();
            while (position < json.length()) {
                char c = json.charAt(position++);
                if (c == '"') return out.toString();
                if (c < 0x20) throw error("unescaped control character in string");
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (position >= json.length()) break;
                char escape = json.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(unicodeEscape());
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private char unicodeEscape() {
            if (position + 4 > json.length()) throw error("truncated \\u escape");
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(json.charAt(position++), 16);
                if (digit < 0) throw error("invalid \\u escape");
                value = value * 16 + digit;
            }
            return (char) value;
        }

        private Value number() {
            int start = position;
            consume('-');
            // A leading zero is never followed by more integer digits.
            if (!consume('0') && !digits()) throw error("invalid number");
            if (consume('.') && !digits()) throw error("invalid fraction");
            if (consume('e') || consume('E')) {
                if (!consume('+')) consume('-');
                if (!digits()) throw error("invalid exponent");
            }
            return new JsonNumber(json.substring(start, position));
        }

        private boolean digits() {
            int start = position;
            while (position < json.length() && json.charAt(position) >= '0' && json.charAt(position) <= '9') {
                position++;
            }
            return position > start;
        }

        private Value literal(String word, Value value) {
            if (!json.startsWith(word, position)) throw error("unexpected literal");
            position += word.length();
            return value;
        }

        private boolean consume(char expected) {
            if (position < json.length() && json.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < json.length()) {
                char c = json.charAt(position);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return;
                position++;
            }
        }

        private IllegalArgumentException error(String problem) {
            return new IllegalArgumentException("Invalid JSON at offset " + position + ": " + problem);
        }
    }
}
