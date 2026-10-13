package com.github.theholywaffle.teamspeak3.commands.response;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2017 Bert De Geyter, Roger Baumgartner
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
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

        String[] rawRows = rawResponse.split("\\|", -1);
        Map<String, String> shared = parseMap(rawRows[0]);
        List<Wrapper> rows = new ArrayList<>(rawRows.length);
        rows.add(new Wrapper(shared));
        for (int i = 1; i < rawRows.length; i++) {
            Map<String, String> row = new LinkedHashMap<>(shared);
            row.putAll(parseMap(rawRows[i]));
            rows.add(new Wrapper(row));
        }

        return new DefaultArrayResponse(rows, rawResponse);
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
        String error = rawError.substring("error ".length());
        Map<String, String> errorMap = parseMap(error);
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

    private static Map<String, String> parseMap(String rawMap) {
        if (rawMap.isEmpty()) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        for (String param : rawMap.split(" ", -1)) {
            if (param.isEmpty()) continue;
            int pos = param.indexOf('=');
            if (pos == -1) map.put(CommandEncoding.decode(param), "");
            else
                map.put(
                        CommandEncoding.decode(param.substring(0, pos)),
                        CommandEncoding.decode(param.substring(pos + 1)));
        }
        return map;
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

    public String getRawResponse() {
        return rawResponse;
    }

    @Override
    public String toString() {
        return rawResponse;
    }
}
