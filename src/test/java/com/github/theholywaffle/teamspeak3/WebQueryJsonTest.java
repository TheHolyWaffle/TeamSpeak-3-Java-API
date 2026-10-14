package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebQueryJsonTest {

    @Test
    void parsesTheDocumentedResponseShape() {
        var root = WebQueryJson.parse(
                " {\"body\":[{\"cid\":\"1\",\"n\":12,\"neg\":-0.5e+3,\"t\":true,\"f\":false,\"z\":null}],"
                        + "\"status\":{\"code\":0,\"message\":\"ok\"}}\r\n");
        var members = root.members("root");
        assertEquals(List.of("body", "status"), List.copyOf(members.keySet()));
        var row = members.get("body").elements("body").getFirst().members("row");
        assertEquals("1", row.get("cid").text("cid"));
        assertEquals("12", row.get("n").text("n"));
        assertEquals("-0.5e+3", row.get("neg").text("neg"));
        assertEquals("1", row.get("t").text("t"));
        assertEquals("0", row.get("f").text("f"));
        assertEquals("", row.get("z").text("z"));
        assertEquals(List.of(), WebQueryJson.parse("[ ]").elements("empty"));
        assertTrue(WebQueryJson.parse("{}").members("empty").isEmpty());
    }

    @Test
    void decodesEveryEscape() {
        String decoded = WebQueryJson.parse("\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u00e9 \\ud83d\\ude00 \\u0000\"")
                .text("value");
        assertEquals("q\" b\\ s/ \b\f\n\r\t é 😀 \0", decoded);
    }

    @Test
    void quotingRoundTripsControlCharactersAndLoneSurrogates() {
        for (String value : List.of(
                "", "plain", "a b|c\\s\\p", "q\"\\/", "\b\f\n\r\t\u0001\u001f", "é😀", "lone \ud800 x", "end \udc00")) {
            String json = WebQueryJson.quote(value, new StringBuilder()).toString();
            assertTrue(json.chars().noneMatch(c -> c < 0x20), json);
            assertEquals(value, WebQueryJson.parse(json).text("value"), json);
        }
        assertEquals(
                "\"\\ud800\"", WebQueryJson.quote("\ud800", new StringBuilder()).toString());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "{",
                "}",
                "{\"a\"}",
                "{\"a\":}",
                "{\"a\":1,}",
                "{a:1}",
                "[1,]",
                "[1 2]",
                "\"unterminated",
                "\"bad \\x escape\"",
                "\"short \\u12\"",
                "\"bad \\u12G4\"",
                "\"raw \n newline\"",
                "01",
                "-",
                "1.",
                "1e",
                "+1",
                "tru",
                "nul",
                "{} {}",
                "<html>",
                "\"trailing \\"
            })
    void rejectsMalformedJson(String json) {
        assertThrows(IllegalArgumentException.class, () -> WebQueryJson.parse(json));
    }

    @Test
    void boundsNesting() {
        assertDoesNotThrow(() -> WebQueryJson.parse("[".repeat(32) + "]".repeat(32)));
        var error = assertThrows(
                IllegalArgumentException.class, () -> WebQueryJson.parse("[".repeat(10_000) + "]".repeat(10_000)));
        assertTrue(error.getMessage().contains("nesting"));
    }

    @Test
    void shapeMismatchesNameTheValueAndNeverItsContent() {
        var text = WebQueryJson.parse("\"secret\"");
        assertEquals(
                "body is not a JSON array",
                assertThrows(IllegalArgumentException.class, () -> text.elements("body"))
                        .getMessage());
        assertThrows(IllegalArgumentException.class, () -> text.members("status"));
        assertThrows(
                IllegalArgumentException.class, () -> WebQueryJson.parse("[]").text("cid"));
        assertThrows(
                IllegalArgumentException.class, () -> WebQueryJson.parse("{}").text("cid"));
        var malformed = assertThrows(IllegalArgumentException.class, () -> WebQueryJson.parse("{\"apikey\":secret}"));
        assertFalse(malformed.getMessage().contains("secret"));
    }
}
