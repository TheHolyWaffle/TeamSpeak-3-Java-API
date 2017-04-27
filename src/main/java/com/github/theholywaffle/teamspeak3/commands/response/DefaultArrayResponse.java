package com.github.theholywaffle.teamspeak3.commands.response;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class DefaultArrayResponse {

	private final List<Wrapper> array;
	private String rawResponse;

	public DefaultArrayResponse() {
		rawResponse = "";
		array = Collections.emptyList();
	}

	public DefaultArrayResponse(String raw) {
		rawResponse = raw;
		array = new LinkedList<>();
		appendWrappers(raw);
	}

	public void appendResponse(String raw) {
		rawResponse += "|" + raw;
		appendWrappers(raw);
	}

	private void appendWrappers(String raw) {
		final StringTokenizer tkn = new StringTokenizer(raw, "|", false);
		while (tkn.hasMoreTokens()) {
			final Map<String, String> responseMap;
			final Map<String, String> parsedTokens = parse(tkn.nextToken());

			if (array.isEmpty()) {
				// First response, just use the parsed tokens
				responseMap = parsedTokens;
			} else {
				// Array response: use "default" values from the first response
				responseMap = new HashMap<>(array.get(0).getMap());
				responseMap.putAll(parsedTokens);
			}

			final Wrapper wrapper = new Wrapper(responseMap);
			array.add(wrapper);
		}
	}

	private static Map<String, String> parse(String raw) {
		final StringTokenizer st = new StringTokenizer(raw, " ", false);
		final Map<String, String> options = new HashMap<>();

		while (st.hasMoreTokens()) {
			final String tmp = st.nextToken();
			final int pos = tmp.indexOf("=");

			if (pos == -1) {
				final String valuelessKey = CommandEncoding.decode(tmp);
				options.put(valuelessKey, "");
			} else {
				final String key = CommandEncoding.decode(tmp.substring(0, pos));
				final String value = CommandEncoding.decode(tmp.substring(pos + 1));
				options.put(key, value);
			}
		}
		return options;
	}

	public List<Wrapper> getArray() {
		return array;
	}

	public Wrapper getFirstResponse() {
		if (array.size() == 0) return Wrapper.EMPTY;
		return array.get(0);
	}

	public String getRawResponse() {
		return rawResponse;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		for (final Wrapper wrapper : array) {
			str.append(wrapper.getMap()).append(" | ");
		}

		str.setLength(str.length() - " | ".length());
		return str.toString();
	}
}
