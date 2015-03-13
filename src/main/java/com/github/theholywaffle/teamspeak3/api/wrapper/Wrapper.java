package com.github.theholywaffle.teamspeak3.api.wrapper;

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

import com.github.theholywaffle.teamspeak3.api.Property;

import java.util.Map;

public class Wrapper {

	private final Map<String, String> map;

	public Wrapper(Map<String, String> map) {
		this.map = map;
	}

	public Map<String, String> getMap() {
		return map;
	}

	public boolean getBoolean(String str) {
		return getInt(str) == 1;
	}

	public boolean getBoolean(Property p) {
		return getBoolean(p.getName());
	}

	public double getDouble(String str) {
		final String value = get(str);
		if (value == null || value.isEmpty()) {
			return -1D;
		}
		return Double.valueOf(value);
	}

	public double getDouble(Property p) {
		return getDouble(p.getName());
	}

	public long getLong(String str) {
		final String value = get(str);
		if (value == value || value.isEmpty()) {
			return -1L;
		}
		return Long.valueOf(value);
	}

	public long getLong(Property p) {
		return getLong(p.getName());
	}

	public int getInt(String str) {
		final String value = get(str);
		if (value == null || value.isEmpty()) {
			return -1;
		}
		return Integer.valueOf(value);
	}

	public int getInt(Property p) {
		return getInt(p.getName());
	}

	/**
	 * Only use this if you know what you are doing.
	 */
	public String get(String str) {
		final String result = map.get(str);
		return result != null ? result : "";
	}

	/**
	 * Only use this if you know what you are doing.
	 */
	public String get(Property p) {
		return get(p.getName());
	}

	@Override
	public String toString() {
		return map.toString();
	}

}
