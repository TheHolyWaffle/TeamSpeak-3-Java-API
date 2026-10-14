package com.github.theholywaffle.teamspeak3.commands.response;

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import com.github.theholywaffle.teamspeak3.commands.ProtocolRedactor;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultArrayResponse {

    /**
     * Parses rows separated by pipes, inheriting shared fields from the first row.
     * Empty rows, including trailing rows, inherit those same fields. Null or
     * empty input represents no rows; valueless fields represent empty strings.
     *
     * @param rawResponse the encoded response
     * @return the parsed response
     */
    public static DefaultArrayResponse parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) return EMPTY;
        return new DefaultArrayResponse(inherit(CommandEncoding.decodeEntries(rawResponse)), rawResponse);
    }

    /**
     * Builds a response from rows that are already decoded, such as Web Query JSON, with the same
     * inheritance of shared fields from the first row as {@link #parse(String)}.
     *
     * @param rows decoded rows, in received order
     * @return the response, whose {@link #getRawResponse()} is the rows' ServerQuery encoding
     */
    public static DefaultArrayResponse fromRows(List<Map<String, String>> rows) {
        if (rows.isEmpty()) return EMPTY;
        var encoded = new ArrayParameter(rows.stream()
                        .map(row -> row.entrySet().stream()
                                .<Parameter>map(field -> new KeyValueParam(field.getKey(), field.getValue()))
                                .toList())
                        .toList())
                .encoded();
        return new DefaultArrayResponse(inherit(rows), encoded);
    }

    private static List<Wrapper> inherit(List<Map<String, String>> rows) {
        Map<String, String> shared = rows.getFirst();
        List<Wrapper> wrappers = new ArrayList<>(rows.size());
        wrappers.add(new Wrapper(shared));
        for (Map<String, String> fields : rows.subList(1, rows.size())) {
            Map<String, String> row = new LinkedHashMap<>(shared);
            row.putAll(fields);
            wrappers.add(new Wrapper(row));
        }
        return wrappers;
    }

    /**
     * Parses a terminating error line. IDs 0 and 1281 represent success.
     *
     * @param rawError the encoded error line
     * @return the parsed error
     * @throws IllegalArgumentException if the prefix or nonnegative integer ID is missing or invalid
     */
    public static QueryError parseError(String rawError) {
        if (rawError == null || !rawError.startsWith("error ")) {
            throw new IllegalArgumentException("Expected an error line");
        }
        Map<String, String> errorMap = CommandEncoding.decodeEntry(rawError.substring("error ".length()));
        String id = errorMap.get("id");
        try {
            if (id == null || Integer.parseInt(id) < 0) {
                throw new IllegalArgumentException("Error line requires a nonnegative integer id");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error line requires a nonnegative integer id", e);
        }
        return new QueryError(errorMap);
    }

    private static final DefaultArrayResponse EMPTY = new DefaultArrayResponse(List.of(), "");

    private final List<Wrapper> responses;
    private final String rawResponse;

    private DefaultArrayResponse(List<Wrapper> responses, String rawResponse) {
        this.responses = List.copyOf(responses);
        this.rawResponse = rawResponse;
    }

    /** @return the immutable rows, in received order */
    public List<Wrapper> getResponses() {
        return responses;
    }

    /** @return the first row, or empty if the response has no rows */
    public Optional<Wrapper> getFirstResponse() {
        return responses.stream().findFirst();
    }

    /** @return the encoded response, including secrets; {@link #toString()} redacts them */
    public String getRawResponse() {
        return rawResponse;
    }

    /** @return the encoded response with {@linkplain ProtocolRedactor secrets} redacted */
    @Override
    public String toString() {
        return ProtocolRedactor.redactLine(rawResponse);
    }
}
