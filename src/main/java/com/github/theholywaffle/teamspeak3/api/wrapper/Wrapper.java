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

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.Property;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

/**
 * An immutable snapshot of the properties of one ServerQuery response row or event.
 * <p>
 * The properties are copied on construction and every received property is retained, including
 * properties this library does not know. Typed model getters and the {@code find*} / {@code require*}
 * accessors keep three cases apart:
 * </p><ul>
 * <li><b>missing</b>: {@code find*} returns an empty optional; {@code require*} throws a
 * {@link TS3InvalidValueException} whose {@linkplain TS3InvalidValueException#getRawValue() raw value} is empty</li>
 * <li><b>malformed</b>: both throw a {@link TS3InvalidValueException} carrying the received raw value</li>
 * <li><b>unknown</b> enum codes: typed getters return the enum's {@code UNKNOWN} constant;
 * the raw code stays available from {@link #getMap()}</li>
 * </ul>
 * <p>
 * Values are interpreted when a getter is called, so a new or changed property only affects
 * the getters that read it. A property without a value ({@code key} instead of {@code key=value})
 * is an empty string; numeric and boolean accessors treat it as missing.
 * </p>
 */
public class Wrapper {

	private static final String UNSIGNED_64_MAX = "18446744073709551615";

	private final Map<String, String> map;

	/**
	 * Creates an immutable copy of the given properties.
	 *
	 * @param map
	 * 		the properties to copy; neither the map nor its keys or values may be {@code null}
	 */
	public Wrapper(Map<String, String> map) {
		LinkedHashMap<String, String> copy = new LinkedHashMap<>(map);
		copy.forEach((key, value) -> {
			Objects.requireNonNull(key, "property name");
			Objects.requireNonNull(value, "property value");
		});
		this.map = Collections.unmodifiableMap(copy);
	}

	/**
	 * Gets every received property, including properties without a typed getter.
	 *
	 * @return an unmodifiable map in response order
	 */
	public final Map<String, String> getMap() {
		return map;
	}

	/** @return the raw value, or empty if the property is missing */
	public final Optional<String> find(String propertyName) {
		return Optional.ofNullable(map.get(propertyName));
	}

	/** @return the raw value, or empty if the property is missing */
	public final Optional<String> find(Property property) {
		return find(property.getName());
	}

	/** @throws TS3InvalidValueException if the property is missing */
	public final String require(String propertyName) {
		String value = map.get(propertyName);
		if (value == null) throw new TS3InvalidValueException(propertyName, null, "present", null);
		return value;
	}

	/** @throws TS3InvalidValueException if the property is missing */
	public final String require(Property property) {
		return require(property.getName());
	}

	/** @throws TS3InvalidValueException if the value is not an integer */
	public final OptionalInt findInt(String propertyName) {
		return parse(propertyName, "an integer", Integer::parseInt).map(OptionalInt::of).orElse(OptionalInt.empty());
	}

	/** @throws TS3InvalidValueException if the value is not an integer */
	public final OptionalInt findInt(Property property) {
		return findInt(property.getName());
	}

	/** @throws TS3InvalidValueException if the property is missing or not an integer */
	public final int requireInt(String propertyName) {
		return required(propertyName, "an integer", Integer::parseInt);
	}

	/** @throws TS3InvalidValueException if the property is missing or not an integer */
	public final int requireInt(Property property) {
		return requireInt(property.getName());
	}

	/** @throws TS3InvalidValueException if the value is not a 64-bit integer */
	public final OptionalLong findLong(String propertyName) {
		return parse(propertyName, "a 64-bit integer", Long::parseLong).map(OptionalLong::of).orElse(OptionalLong.empty());
	}

	/** @throws TS3InvalidValueException if the value is not a 64-bit integer */
	public final OptionalLong findLong(Property property) {
		return findLong(property.getName());
	}

	/** @throws TS3InvalidValueException if the property is missing or not a 64-bit integer */
	public final long requireLong(String propertyName) {
		return required(propertyName, "a 64-bit integer", Long::parseLong);
	}

	/** @throws TS3InvalidValueException if the property is missing or not a 64-bit integer */
	public final long requireLong(Property property) {
		return requireLong(property.getName());
	}

	/** @throws TS3InvalidValueException if the value is not a number */
	public final OptionalDouble findDouble(String propertyName) {
		return parse(propertyName, "a number", Wrapper::parseDouble).map(OptionalDouble::of).orElse(OptionalDouble.empty());
	}

	/** @throws TS3InvalidValueException if the value is not a number */
	public final OptionalDouble findDouble(Property property) {
		return findDouble(property.getName());
	}

	/** @throws TS3InvalidValueException if the property is missing or not a number */
	public final double requireDouble(String propertyName) {
		return required(propertyName, "a number", Wrapper::parseDouble);
	}

	/** @throws TS3InvalidValueException if the property is missing or not a number */
	public final double requireDouble(Property property) {
		return requireDouble(property.getName());
	}

	/** @throws TS3InvalidValueException if the value is neither {@code 0} nor {@code 1} */
	public final Optional<Boolean> findBoolean(String propertyName) {
		return parse(propertyName, "0 or 1", Wrapper::parseBoolean);
	}

	/** @throws TS3InvalidValueException if the value is neither {@code 0} nor {@code 1} */
	public final Optional<Boolean> findBoolean(Property property) {
		return findBoolean(property.getName());
	}

	/** @throws TS3InvalidValueException if the property is missing or neither {@code 0} nor {@code 1} */
	public final boolean requireBoolean(String propertyName) {
		return required(propertyName, "0 or 1", Wrapper::parseBoolean);
	}

	/** @throws TS3InvalidValueException if the property is missing or neither {@code 0} nor {@code 1} */
	public final boolean requireBoolean(Property property) {
		return requireBoolean(property.getName());
	}

	/** Text where an empty value means not set. */
	protected final Optional<String> findText(String propertyName) {
		return find(propertyName).filter(value -> !value.isEmpty());
	}

	protected final Optional<String> findText(Property property) {
		return findText(property.getName());
	}

	protected final Instant requireInstant(String propertyName) {
		return Instant.ofEpochSecond(requireLong(propertyName));
	}

	protected final Instant requireInstant(Property property) {
		return requireInstant(property.getName());
	}

	/** Epoch seconds, where {@code 0} means the server has not recorded the time. */
	protected final Optional<Instant> requireTimestamp(String propertyName) {
		long seconds = requireLong(propertyName);
		return seconds == 0 ? Optional.empty() : Optional.of(Instant.ofEpochSecond(seconds));
	}

	protected final Optional<Instant> requireTimestamp(Property property) {
		return requireTimestamp(property.getName());
	}

	protected final Duration requireSeconds(String propertyName) {
		return Duration.ofSeconds(requireLong(propertyName));
	}

	protected final Duration requireSeconds(Property property) {
		return requireSeconds(property.getName());
	}

	protected final Duration requireMillis(String propertyName) {
		return Duration.ofMillis(requireLong(propertyName));
	}

	protected final Duration requireMillis(Property property) {
		return requireMillis(property.getName());
	}

	/** An unsigned 64-bit limit, where the maximum value means unlimited. */
	protected final OptionalLong requireLimit(Property property) {
		String name = property.getName();
		if (UNSIGNED_64_MAX.equals(map.get(name))) return OptionalLong.empty();
		return OptionalLong.of(requireLong(name));
	}

	/** @throws TS3InvalidValueException if the property is missing or not a nonnegative integer */
	public final ClientId requireClientId(String propertyName) {
		return new ClientId(requireNonNegative(propertyName));
	}

	/** @throws TS3InvalidValueException if the property is missing or not a positive integer */
	public final ClientDatabaseId requireDatabaseId(String propertyName) {
		int id = requireNonNegative(propertyName);
		if (id == 0) throw new TS3InvalidValueException(propertyName, "0", "a positive client database ID", null);
		return new ClientDatabaseId(id);
	}

	/** @throws TS3InvalidValueException if the property is missing or not a positive integer */
	public final ClientDatabaseId requireDatabaseId(Property property) {
		return requireDatabaseId(property.getName());
	}

	/** A client database ID where {@code 0} means none, such as the server as invoker. */
	protected final Optional<ClientDatabaseId> findDatabaseId(String propertyName) {
		OptionalInt id = findInt(propertyName);
		if (id.isEmpty() || id.getAsInt() == 0) return Optional.empty();
		if (id.getAsInt() < 0) throw new TS3InvalidValueException(propertyName, map.get(propertyName), "a client database ID", null);
		return Optional.of(new ClientDatabaseId(id.getAsInt()));
	}

	private int requireNonNegative(String propertyName) {
		int value = requireInt(propertyName);
		if (value < 0) throw new TS3InvalidValueException(propertyName, map.get(propertyName), "a nonnegative ID", null);
		return value;
	}

	/** Comma-separated integers, where an empty value is an empty list. */
	protected final List<Integer> requireIntList(Property property) {
		String name = property.getName();
		String value = require(name);
		if (value.isEmpty()) return List.of();
		try {
			return Arrays.stream(value.split(",")).map(Integer::valueOf).toList();
		} catch (NumberFormatException failure) {
			throw new TS3InvalidValueException(name, value, "a comma-separated integer list", failure);
		}
	}

	/** Maps a code with the given decoder, which returns the enum's {@code UNKNOWN} constant for unknown codes. */
	protected final <E extends Enum<E>> E requireCode(String propertyName, Function<Integer, E> decoder) {
		return decoder.apply(requireInt(propertyName));
	}

	protected final <E extends Enum<E>> E requireCode(Property property, Function<Integer, E> decoder) {
		return requireCode(property.getName(), decoder);
	}

	private <T> T required(String propertyName, String expected, Function<String, T> parser) {
		return parse(propertyName, expected, parser).orElseThrow(
			() -> new TS3InvalidValueException(propertyName, map.get(propertyName), expected, null));
	}

	private <T> Optional<T> parse(String propertyName, String expected, Function<String, T> parser) {
		String value = map.get(propertyName);
		if (value == null || value.isEmpty()) return Optional.empty();
		try {
			return Optional.of(parser.apply(value));
		} catch (IllegalArgumentException failure) {
			throw new TS3InvalidValueException(propertyName, value, expected, failure);
		}
	}

	private static double parseDouble(String value) {
		double parsed = Double.parseDouble(value);
		if (!Double.isFinite(parsed)) throw new NumberFormatException("Not finite");
		return parsed;
	}

	private static Boolean parseBoolean(String value) {
		return switch (value) {
			case "1" -> Boolean.TRUE;
			case "0" -> Boolean.FALSE;
			default -> throw new IllegalArgumentException("Not a flag");
		};
	}

	@Override
	public boolean equals(Object other) {
		return other != null && other.getClass() == getClass() && map.equals(((Wrapper) other).map);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + map;
	}
}
