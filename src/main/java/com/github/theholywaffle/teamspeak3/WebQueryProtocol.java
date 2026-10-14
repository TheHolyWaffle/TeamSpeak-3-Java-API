package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3AuthenticationFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3HttpException;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Translates between ServerQuery commands and the TS3 Web Query HTTP/JSON format documented in the
 * server's {@code doc/webquery.md}.
 */
final class WebQueryProtocol {

    static final String API_KEY_HEADER = "x-api-key";

    private WebQueryProtocol() {}

    /** @return why the server cannot execute {@code commandName} over Web Query, if it cannot */
    static Optional<String> unsupported(String commandName) {
        String reason = switch (commandName) {
            case "use" ->
                "Web Query selects the virtual server in each request URL; use TS3WebQuery.getApi(int)"
                        + " or getApiByPort(int)";
            case "login", "logout" -> "Web Query authenticates every request with its API key";
            case "quit" -> "Web Query has no session to quit; close the TS3WebQuery";
            case "servernotifyregister", "servernotifyunregister" ->
                "Web Query has no notification stream; use a RAW or SSH TS3Query for events";
            case "help" -> "Web Query does not serve command help";
            default ->
                commandName.startsWith("ft")
                        ? "Web Query does not support file commands; use a RAW or SSH TS3Query"
                        : null;
        };
        return Optional.ofNullable(reason).map(text -> commandName + " is unsupported: " + text);
    }

    /**
     * @param scope empty for the instance, {@code "1/"} for a virtual server ID or {@code "byport/9987/"}
     * @return the request URI below the base URI
     */
    static URI target(URI baseUri, String scope, String commandName) {
        return baseUri.resolve(scope + commandName);
    }

    /**
     * Converts the command's parameters to the JSON body: one object, or one object per pipe-separated
     * entry where the first holds the shared parameters. Options become members with empty values.
     */
    static String body(Command command) {
        String line = command.encoded();
        String parameters = line.length() > command.getName().length()
                ? line.substring(command.getName().length() + 1)
                : "";
        String[] entries = parameters.split("\\|", -1);
        var json = new StringBuilder();
        if (entries.length > 1) json.append('[');
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) json.append(',');
            appendObject(entries[i], json);
        }
        if (entries.length > 1) json.append(']');
        return json.toString();
    }

    private static void appendObject(String entry, StringBuilder json) {
        json.append('{');
        boolean first = true;
        for (String token : entry.split(" ", -1)) {
            if (token.isEmpty()) continue;
            if (!first) json.append(',');
            first = false;
            int equals = token.indexOf('=');
            String key = CommandEncoding.decode(equals < 0 ? token : token.substring(0, equals));
            String value = equals < 0 ? "" : CommandEncoding.decode(token.substring(equals + 1));
            WebQueryJson.quote(value, WebQueryJson.quote(key, json).append(':'));
        }
        json.append('}');
    }

    /**
     * Converts a response into the rows a ServerQuery response would carry, so models, redaction and
     * {@link DefaultArrayResponse#getRawResponse()} behave the same for every transport.
     *
     * @throws TS3AuthenticationFailedException if HTTP 401/403 carries a ServerQuery error
     * @throws TS3CommandFailedException if the status reports any other ServerQuery error
     * @throws TS3HttpException if the response has no usable status
     */
    static DefaultArrayResponse response(String commandName, int httpStatus, String body) {
        WebQueryJson.Value root;
        try {
            root = WebQueryJson.parse(body);
        } catch (IllegalArgumentException e) {
            throw new TS3HttpException(commandName, httpStatus, "the body is not JSON", e);
        }
        QueryError error;
        List<Map<String, String>> rows;
        try {
            Map<String, WebQueryJson.Value> response = root.members("response");
            error = status(response.get("status"));
            rows = error.isSuccessful() ? rows(response.get("body")) : List.of();
        } catch (IllegalArgumentException e) {
            throw new TS3HttpException(commandName, httpStatus, "the JSON lacks a valid status or body", e);
        }
        if (!error.isSuccessful()) {
            if (httpStatus == 401 || httpStatus == 403) throw new TS3AuthenticationFailedException(error, commandName);
            throw new TS3CommandFailedException(error, commandName);
        }
        if (httpStatus / 100 != 2) {
            throw new TS3HttpException(commandName, httpStatus, "a successful status with an HTTP error", null);
        }
        var raw = new StringJoiner("|");
        for (Map<String, String> row : rows) {
            var fields = new StringJoiner(" ");
            row.forEach((key, value) -> fields.add(CommandEncoding.encode(key) + "=" + CommandEncoding.encode(value)));
            raw.add(fields.toString());
        }
        return DefaultArrayResponse.parse(raw.toString());
    }

    private static QueryError status(WebQueryJson.Value value) {
        if (value == null) throw new IllegalArgumentException("Missing status");
        Map<String, String> error = new LinkedHashMap<>();
        value.members("status").forEach((key, member) -> {
            String name = switch (key) {
                case "code" -> "id";
                case "message" -> "msg";
                case "extra_message" -> "extra_msg";
                default -> key;
            };
            error.put(name, member.text("status " + key));
        });
        String id = error.get("id");
        if (id == null || !id.matches("\\d{1,9}")) throw new IllegalArgumentException("Status code is not a number");
        error.putIfAbsent("msg", "");
        return new QueryError(error);
    }

    private static List<Map<String, String>> rows(WebQueryJson.Value body) {
        if (body == null) return List.of();
        return body.elements("body").stream()
                .map(row -> {
                    Map<String, String> fields = new LinkedHashMap<>();
                    row.members("row").forEach((key, value) -> fields.put(key, value.text(key)));
                    return fields;
                })
                .toList();
    }
}
