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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import com.github.theholywaffle.teamspeak3.StringUtil;

public class DefaultArrayResponse {

	private final List<HashMap<String, String>> array = new ArrayList<>();

	public DefaultArrayResponse(String raw) {
		final StringTokenizer tkn = new StringTokenizer(raw, "|", false);

		while (tkn.hasMoreTokens()) {
			array.add(parse(tkn.nextToken()));
		}
	}

	private HashMap<String, String> parse(String raw) {
		final StringTokenizer st = new StringTokenizer(raw, " ", false);
		final HashMap<String, String> options = new HashMap<>();

		while (st.hasMoreTokens()) {
			final String tmp = st.nextToken();
			final int pos = tmp.indexOf("=");

			if (pos == -1) {
				options.put(tmp, "");
			} else {
				options.put(StringUtil.decode(tmp.substring(0, pos)),
						StringUtil.decode(tmp.substring(pos + 1)));
			}
		}
		return options;
	}

	public List<HashMap<String, String>> getArray() {
		return array;
	}

	@Override
	public String toString() {
		String str = "";
		for (final HashMap<String, String> opt : array) {
			str += opt + " | ";
		}
		return str;
	}

}
