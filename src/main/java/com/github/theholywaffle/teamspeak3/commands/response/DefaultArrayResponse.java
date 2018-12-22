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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultArrayResponse {

	public static DefaultArrayResponse parse(String rawResponse) {
		if (rawResponse == null || rawResponse.isEmpty()) return EMPTY;

		String[] rawMaps = rawResponse.split("\\|");
		List<Wrapper> responses = new ArrayList<>(rawMaps.length);
		if (rawMaps.length == 0) return new DefaultArrayResponse(responses, rawResponse);

		// First response, just use the parsed map
		Map<String, String> firstResponse = parseMap(rawMaps[0]);
		responses.add(new Wrapper(firstResponse));

		// Array response: use "default" values from the first response, then add the parsed map
		for (int i = 1; i < rawMaps.length; ++i) {
			final Map<String, String> responseMap = new HashMap<>(firstResponse);
			final Map<String, String> ithResponse = parseMap(rawMaps[i]);
			responseMap.putAll(ithResponse);

			responses.add(new Wrapper(responseMap));
		}

		return new DefaultArrayResponse(responses, rawResponse);
	}

	public static QueryError parseError(String rawError) {
		String error = rawError.substring("error ".length());
		Map<String, String> errorMap = parseMap(error);
		return new QueryError(errorMap);
	}

	private static Map<String, String> parseMap(String rawMap) {
		if (rawMap == null || rawMap.isEmpty()) return Collections.emptyMap();

		final String[] parameters = rawMap.split(" ");
		final Map<String, String> map = new HashMap<>(parameters.length);

		for (String param : parameters) {
			if (param.isEmpty()) continue;
			final int pos = param.indexOf("=");

			if (pos == -1) {
				final String valuelessKey = CommandEncoding.decode(param);
				map.put(valuelessKey, "");
			} else {
				final String key = CommandEncoding.decode(param.substring(0, pos));
				final String value = CommandEncoding.decode(param.substring(pos + 1));
				map.put(key, value);
			}
		}

		return map;
	}

	private static final DefaultArrayResponse EMPTY = new DefaultArrayResponse(Collections.emptyList(), "");

	private final List<Wrapper> responses;
	private final String rawResponse;

	private DefaultArrayResponse(List<Wrapper> responses, String rawResponse) {
		this.responses = Collections.unmodifiableList(responses);
		this.rawResponse = rawResponse;
	}

	public List<Wrapper> getResponses() {
		return responses;
	}

	public Wrapper getFirstResponse() {
		if (responses.isEmpty()) return Wrapper.EMPTY;
		return responses.get(0);
	}

	public String getRawResponse() {
		return rawResponse;
	}

	@Override
	public String toString() {
		return rawResponse;
	}
}
