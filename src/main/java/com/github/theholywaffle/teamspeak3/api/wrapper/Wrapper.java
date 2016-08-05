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

import java.util.Collections;
import java.util.Map;

/**
 * A wrapper class around a {@link Map}.
 * <p>
 * We use wrapper classes instead of just passing around plain Maps because
 * </p><ul>
 * <li>it's more descriptive</li>
 * <li>there's no confusion between types (e.g. {@code Client} and {@code Channel})</li>
 * <li>we can create getters for the specific use-cases</li>
 * <li>we don't have to check for {@code null} each time</li>
 * </ul>
 */
public class Wrapper {

	public static final Wrapper EMPTY = new Wrapper(Collections.<String, String>emptyMap());

	private final Map<String, String> map;

	/**
	 * Creates a new wrapper around the given map.
	 *
	 * @param map
	 * 		the Map to abstract, cannot be {@code null}
	 */
	public Wrapper(Map<String, String> map) {
		this.map = map;
	}

	/**
	 * Gets the map underlying this {@code Wrapper}.
	 *
	 * @return the underlying map
	 */
	public Map<String, String> getMap() {
		return map;
	}

	/**
	 * Gets a property as a boolean from the underlying map.
	 * Returns {@code true} if the property exists in the map and its value is {@code "1"}.
	 *
	 * @param propertyName
	 * 		the name of the property
	 *
	 * @return the boolean value of the property or {@code false} if the property doesn't exist
	 */
	public boolean getBoolean(String propertyName) {
		return getInt(propertyName) == 1;
	}

	/**
	 * Gets a property as a boolean from the underlying map.
	 * Returns {@code true} if the property exists in the map and its value is {@code "1"}.
	 *
	 * @param property
	 * 		the property
	 *
	 * @return the boolean value of the property or {@code false} if the property doesn't exist
	 */
	public boolean getBoolean(Property property) {
		return getBoolean(property.getName());
	}

	/**
	 * Gets a property as a double from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1.0} is returned.
	 *
	 * @param propertyName
	 * 		the name of the property
	 *
	 * @return the double value of the property or {@code -1.0} if the property doesn't exist
	 */
	public double getDouble(String propertyName) {
		final String value = get(propertyName);
		if (value == null || value.isEmpty()) {
			return -1D;
		}
		return Double.valueOf(value);
	}

	/**
	 * Gets a property as a double from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1.0} is returned.
	 *
	 * @param property
	 * 		the property
	 *
	 * @return the double value of the property or {@code -1.0} if the property doesn't exist
	 */
	public double getDouble(Property property) {
		return getDouble(property.getName());
	}

	/**
	 * Gets a property as a long from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1} is returned.
	 *
	 * @param propertyName
	 * 		the name of the property
	 *
	 * @return the long value of the property or {@code -1} if the property doesn't exist
	 */
	public long getLong(String propertyName) {
		final String value = get(propertyName);
		if (value == null || value.isEmpty()) {
			return -1L;
		}
		return Long.parseLong(value);
	}

	/**
	 * Gets a property as a long from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1} is returned.
	 *
	 * @param property
	 * 		the property
	 *
	 * @return the long value of the property or {@code -1} if the property doesn't exist
	 */
	public long getLong(Property property) {
		return getLong(property.getName());
	}

	/**
	 * Gets a property as an integer from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1} is returned.
	 *
	 * @param propertyName
	 * 		the name of the property
	 *
	 * @return the integer value of the property or {@code -1} if the property doesn't exist
	 */
	public int getInt(String propertyName) {
		final String value = get(propertyName);
		if (value == null || value.isEmpty()) {
			return -1;
		}
		return Integer.parseInt(value);
	}

	/**
	 * Gets a property as an integer from the underlying map.
	 * If the property doesn't exist in the underlying map, {@code -1} is returned.
	 *
	 * @param property
	 * 		the property
	 *
	 * @return the integer value of the property or {@code -1} if the property doesn't exist
	 */
	public int getInt(Property property) {
		return getInt(property.getName());
	}

	/**
	 * Gets a property as a String from the underlying map.
	 * If the property doesn't exist in the underlying map, an empty String is returned.
	 *
	 * @param propertyName
	 * 		the name of the property
	 *
	 * @return the String value of the property or an empty String if the property doesn't exist
	 */
	public String get(String propertyName) {
		final String result = map.get(propertyName);
		return result != null ? result : "";
	}

	/**
	 * Gets a property as a {@code String} from the underlying map.
	 * If the property doesn't exist in the underlying map, an empty String is returned.
	 *
	 * @param property
	 * 		the property
	 *
	 * @return the String value of the property or an empty String if the property doesn't exist
	 */
	public String get(Property property) {
		return get(property.getName());
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
