package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3AuthenticationFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3HttpException;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.CommandTrait;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.ParameterSink;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
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
    static final String NO_CONNECTION = "Web Query has no connection or session: every request carries the API key"
            + " and addresses the instance or a virtual server in its URL (TS3WebQuery.getApi(int),"
            + " getApiByPort(int)); close the TS3WebQuery instead of quitting";
    static final String NO_HELP = "Web Query does not serve command help";
    static final String NO_EVENTS = "Web Query has no notification stream; use a RAW or SSH TS3Query for events";
    static final String NO_FILES = "Web Query does not support file commands or transfers; use a RAW or SSH TS3Query";
    // Ordered by priority: the first matching trait explains the rejection.
    private static final Map<CommandTrait, String> UNSUPPORTED = new EnumMap<>(Map.of(
            CommandTrait.CONNECTION, NO_CONNECTION,
            CommandTrait.NOTIFICATIONS, NO_EVENTS,
            CommandTrait.FILES, NO_FILES,
            CommandTrait.HELP, NO_HELP));
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .disableHtmlEscaping()
            .create();

    private WebQueryProtocol() {}

    /** @return why the server cannot execute {@code command} over Web Query, if it cannot */
    static Optional<String> unsupported(Command command) {
        return UNSUPPORTED.entrySet().stream()
                .filter(reason -> command.getTraits().contains(reason.getKey()))
                .findFirst()
                .map(reason -> command.getName() + " is unsupported: " + reason.getValue());
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
        var body = new JsonBody();
        for (Parameter parameter : command.getParameters()) parameter.addTo(body);
        return body.toString();
    }

    private static final class JsonBody implements ParameterSink {
        private final List<JsonObject> objects = new ArrayList<>(List.of(new JsonObject()));

        @Override
        public void put(String key, String value) {
            objects.getLast().addProperty(key, value);
        }

        @Override
        public void nextEntry() {
            objects.add(new JsonObject());
        }

        @Override
        public String toString() {
            if (objects.size() == 1) return GSON.toJson(objects.getFirst());
            var array = new JsonArray(objects.size());
            objects.forEach(array::add);
            return GSON.toJson(array);
        }
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
        Response response;
        try {
            response = GSON.fromJson(body, Response.class);
        } catch (JsonParseException e) {
            throw new TS3HttpException(commandName, httpStatus, "the body is not the documented JSON", e);
        }
        if (response == null) throw new TS3HttpException(commandName, httpStatus, "the body is empty", null);
        QueryError error;
        try {
            error = status(response.status());
        } catch (IllegalArgumentException e) {
            throw new TS3HttpException(commandName, httpStatus, "the JSON lacks a valid status", e);
        }
        if (!error.isSuccessful()) {
            if (httpStatus == 401 || httpStatus == 403) throw new TS3AuthenticationFailedException(error, commandName);
            throw new TS3CommandFailedException(error, commandName);
        }
        if (httpStatus / 100 != 2) {
            throw new TS3HttpException(commandName, httpStatus, "a successful status with an HTTP error", null);
        }
        try {
            return DefaultArrayResponse.fromRows(rows(response.body()));
        } catch (IllegalArgumentException e) {
            throw new TS3HttpException(commandName, httpStatus, "the JSON body is not a list of rows", e);
        }
    }

    /** The documented response object; values are text, as in ServerQuery. */
    private record Response(List<Map<String, String>> body, Map<String, String> status) {}

    private static QueryError status(Map<String, String> status) {
        if (status == null) throw new IllegalArgumentException("Missing status");
        Map<String, String> error = new LinkedHashMap<>();
        status.forEach((key, value) -> {
            String name = switch (key) {
                case "code" -> "id";
                case "message" -> "msg";
                case "extra_message" -> "extra_msg";
                default -> key;
            };
            error.put(name, value == null ? "" : value);
        });
        error.putIfAbsent("msg", "");
        var line = new StringJoiner(" ", "error ", "");
        error.forEach((key, value) -> line.add(new KeyValueParam(key, value).encoded()));
        return DefaultArrayResponse.parseError(line.toString());
    }

    /** Gson's rows, with null values read as empty text like ServerQuery's valueless fields. */
    private static List<Map<String, String>> rows(List<Map<String, String>> body) {
        if (body == null) return List.of();
        for (Map<String, String> row : body) {
            if (row == null) throw new IllegalArgumentException("Row is null");
            row.replaceAll((_, value) -> value == null ? "" : value);
        }
        return body;
    }
}
