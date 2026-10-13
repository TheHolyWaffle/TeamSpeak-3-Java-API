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
import com.github.theholywaffle.teamspeak3.commands.ProtocolRedactor;
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
import java.util.function.IntFunction;

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
        LinkedHashMap<String, String> copy = LinkedHashMap.newLinkedHashMap(map.size());
        map.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "property name"), Objects.requireNonNull(value, "property value")));
        this.map = Collections.unmodifiableMap(copy);
    }

    /**
     * Shares the already immutable properties of another wrapper.
     *
     * @param properties
     * 		the wrapper whose properties to use
     */
    protected Wrapper(Wrapper properties) {
        this.map = properties.map;
    }

    /**
     * Gets every received property, including properties without a typed getter.
     *
     * @return an unmodifiable map in response order
     */
    public final Map<String, String> getMap() {
        return map;
    }

    /**
     * Gets a property's raw value.
     *
     * @param propertyName the property name
     * @return the raw value, or empty if the property is missing
     */
    public final Optional<String> find(String propertyName) {
        return Optional.ofNullable(map.get(propertyName));
    }

    /**
     * Gets a property's raw value.
     *
     * @param property the property
     * @return the raw value, or empty if the property is missing
     */
    public final Optional<String> find(Property property) {
        return find(property.getName());
    }

    /**
     * Gets a property's raw value, which must be present.
     *
     * @param propertyName the property name
     * @return the raw value, possibly empty
     * @throws TS3InvalidValueException if the property is missing
     */
    public final String require(String propertyName) {
        String value = map.get(propertyName);
        if (value == null) throw new TS3InvalidValueException(propertyName, null, "present", null);
        return value;
    }

    /**
     * Gets a property's raw value, which must be present.
     *
     * @param property the property
     * @return the raw value, possibly empty
     * @throws TS3InvalidValueException if the property is missing
     */
    public final String require(Property property) {
        return require(property.getName());
    }

    /**
     * Gets a property as an integer.
     *
     * @param propertyName the property name
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not an integer
     */
    public final OptionalInt findInt(String propertyName) {
        return findParsed(propertyName, "an integer", Integer::parseInt)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    /**
     * Gets a property as an integer.
     *
     * @param property the property
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not an integer
     */
    public final OptionalInt findInt(Property property) {
        return findInt(property.getName());
    }

    /**
     * Gets a property as an integer, which must be present.
     *
     * @param propertyName the property name
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not an integer
     */
    public final int requireInt(String propertyName) {
        return requireParsed(propertyName, "an integer", Integer::parseInt);
    }

    /**
     * Gets a property as an integer, which must be present.
     *
     * @param property the property
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not an integer
     */
    public final int requireInt(Property property) {
        return requireInt(property.getName());
    }

    /**
     * Gets a property as a 64-bit integer.
     *
     * @param propertyName the property name
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not a 64-bit integer
     */
    public final OptionalLong findLong(String propertyName) {
        return findParsed(propertyName, "a 64-bit integer", Long::parseLong)
                .map(OptionalLong::of)
                .orElse(OptionalLong.empty());
    }

    /**
     * Gets a property as a 64-bit integer.
     *
     * @param property the property
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not a 64-bit integer
     */
    public final OptionalLong findLong(Property property) {
        return findLong(property.getName());
    }

    /**
     * Gets a property as a 64-bit integer, which must be present.
     *
     * @param propertyName the property name
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not a 64-bit integer
     */
    public final long requireLong(String propertyName) {
        return requireParsed(propertyName, "a 64-bit integer", Long::parseLong);
    }

    /**
     * Gets a property as a 64-bit integer, which must be present.
     *
     * @param property the property
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not a 64-bit integer
     */
    public final long requireLong(Property property) {
        return requireLong(property.getName());
    }

    /**
     * Gets a property as a number.
     *
     * @param propertyName the property name
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not a number
     */
    public final OptionalDouble findDouble(String propertyName) {
        return findParsed(propertyName, "a number", Wrapper::parseDouble)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
    }

    /**
     * Gets a property as a number.
     *
     * @param property the property
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not a number
     */
    public final OptionalDouble findDouble(Property property) {
        return findDouble(property.getName());
    }

    /**
     * Gets a property as a number, which must be present.
     *
     * @param propertyName the property name
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not a number
     */
    public final double requireDouble(String propertyName) {
        return requireParsed(propertyName, "a number", Wrapper::parseDouble);
    }

    /**
     * Gets a property as a number, which must be present.
     *
     * @param property the property
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not a number
     */
    public final double requireDouble(Property property) {
        return requireDouble(property.getName());
    }

    /**
     * Gets a property as a flag ({@code 1} or {@code 0}).
     *
     * @param propertyName the property name
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not 0 or 1
     */
    public final Optional<Boolean> findBoolean(String propertyName) {
        return findParsed(propertyName, "0 or 1", Wrapper::parseBoolean);
    }

    /**
     * Gets a property as a flag ({@code 1} or {@code 0}).
     *
     * @param property the property
     * @return the value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value is not 0 or 1
     */
    public final Optional<Boolean> findBoolean(Property property) {
        return findBoolean(property.getName());
    }

    /**
     * Gets a property as a flag ({@code 1} or {@code 0}), which must be present.
     *
     * @param propertyName the property name
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not 0 or 1
     */
    public final boolean requireBoolean(String propertyName) {
        return requireParsed(propertyName, "0 or 1", Wrapper::parseBoolean);
    }

    /**
     * Gets a property as a flag ({@code 1} or {@code 0}), which must be present.
     *
     * @param property the property
     * @return the value
     * @throws TS3InvalidValueException if the property is missing, has no value or is not 0 or 1
     */
    public final boolean requireBoolean(Property property) {
        return requireBoolean(property.getName());
    }

    /**
     * Gets text that TeamSpeak leaves empty when it is not set.
     *
     * @param propertyName the property name
     * @return the text, or empty if the property is missing or empty
     */
    protected final Optional<String> findText(String propertyName) {
        return find(propertyName).filter(value -> !value.isEmpty());
    }

    /**
     * Gets text that TeamSpeak leaves empty when it is not set.
     *
     * @param property the property
     * @return the text, or empty if the property is missing or empty
     */
    protected final Optional<String> findText(Property property) {
        return findText(property.getName());
    }

    /**
     * Gets a time sent as epoch seconds.
     *
     * @param propertyName the property name
     * @return the time
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Instant requireInstant(String propertyName) {
        return Instant.ofEpochSecond(requireLong(propertyName));
    }

    /**
     * Gets a time sent as epoch seconds.
     *
     * @param property the property
     * @return the time
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Instant requireInstant(Property property) {
        return requireInstant(property.getName());
    }

    /**
     * Gets a time sent as epoch seconds, where {@code 0} means the server has not recorded it.
     *
     * @param propertyName the property name
     * @return the time, or empty for {@code 0}
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Optional<Instant> requireTimestamp(String propertyName) {
        long seconds = requireLong(propertyName);
        return seconds == 0 ? Optional.empty() : Optional.of(Instant.ofEpochSecond(seconds));
    }

    /**
     * Gets a time sent as epoch seconds, where {@code 0} means the server has not recorded it.
     *
     * @param property the property
     * @return the time, or empty for {@code 0}
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Optional<Instant> requireTimestamp(Property property) {
        return requireTimestamp(property.getName());
    }

    /**
     * Gets a duration sent in seconds.
     *
     * @param propertyName the property name
     * @return the duration
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Duration requireSeconds(String propertyName) {
        return Duration.ofSeconds(requireLong(propertyName));
    }

    /**
     * Gets a duration sent in seconds.
     *
     * @param property the property
     * @return the duration
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Duration requireSeconds(Property property) {
        return requireSeconds(property.getName());
    }

    /**
     * Gets a duration sent in milliseconds.
     *
     * @param propertyName the property name
     * @return the duration
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Duration requireMillis(String propertyName) {
        return Duration.ofMillis(requireLong(propertyName));
    }

    /**
     * Gets a duration sent in milliseconds.
     *
     * @param property the property
     * @return the duration
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final Duration requireMillis(Property property) {
        return requireMillis(property.getName());
    }

    /**
     * Gets an unsigned 64-bit limit, where the maximum value 2<sup>64</sup>-1 means unlimited.
     *
     * @param property the property
     * @return the limit, or empty if unlimited
     * @throws TS3InvalidValueException if the property is missing or not a 64-bit integer
     */
    protected final OptionalLong requireLimit(Property property) {
        String name = property.getName();
        if (UNSIGNED_64_MAX.equals(map.get(name))) return OptionalLong.empty();
        return OptionalLong.of(requireLong(name));
    }

    /**
     * Gets a client session ID.
     *
     * @param propertyName the property name
     * @return the client ID
     * @throws TS3InvalidValueException if the property is missing or not a nonnegative integer
     */
    public final ClientId requireClientId(String propertyName) {
        return new ClientId(requireNonNegative(propertyName));
    }

    /**
     * Gets a client session ID.
     *
     * @param property the property
     * @return the client ID
     * @throws TS3InvalidValueException if the property is missing or not a nonnegative integer
     */
    public final ClientId requireClientId(Property property) {
        return requireClientId(property.getName());
    }

    /**
     * Gets an optional client session ID.
     *
     * @param propertyName the property name
     * @return the client ID, or empty if the property is missing or empty
     * @throws TS3InvalidValueException if the value is not a nonnegative integer
     */
    protected final Optional<ClientId> findClientId(String propertyName) {
        OptionalInt id = findInt(propertyName);
        return id.isEmpty() ? Optional.empty() : Optional.of(new ClientId(nonNegative(propertyName, id.getAsInt())));
    }

    /**
     * Gets a client database ID.
     *
     * @param propertyName the property name
     * @return the client database ID
     * @throws TS3InvalidValueException if the property is missing or not a positive integer
     */
    public final ClientDatabaseId requireDatabaseId(String propertyName) {
        int id = requireNonNegative(propertyName);
        if (id == 0) throw new TS3InvalidValueException(propertyName, "0", "a positive client database ID", null);
        return new ClientDatabaseId(id);
    }

    /**
     * Gets a client database ID.
     *
     * @param property the property
     * @return the client database ID
     * @throws TS3InvalidValueException if the property is missing or not a positive integer
     */
    public final ClientDatabaseId requireDatabaseId(Property property) {
        return requireDatabaseId(property.getName());
    }

    /**
     * Gets a client database ID where {@code 0} means none, such as the server as invoker.
     *
     * @param propertyName the property name
     * @return the client database ID, or empty if the property is missing, empty or {@code 0}
     * @throws TS3InvalidValueException if the value is not a nonnegative integer
     */
    protected final Optional<ClientDatabaseId> findDatabaseId(String propertyName) {
        OptionalInt id = findInt(propertyName);
        if (id.isEmpty() || nonNegative(propertyName, id.getAsInt()) == 0) return Optional.empty();
        return Optional.of(new ClientDatabaseId(id.getAsInt()));
    }

    /**
     * Gets a client database ID where {@code 0} means none.
     *
     * @param property the property
     * @return the client database ID, or empty if the property is missing, empty or {@code 0}
     * @throws TS3InvalidValueException if the value is not a nonnegative integer
     */
    protected final Optional<ClientDatabaseId> findDatabaseId(Property property) {
        return findDatabaseId(property.getName());
    }

    private int requireNonNegative(String propertyName) {
        return nonNegative(propertyName, requireInt(propertyName));
    }

    private int nonNegative(String propertyName, int value) {
        if (value < 0)
            throw new TS3InvalidValueException(propertyName, map.get(propertyName), "a nonnegative ID", null);
        return value;
    }

    /**
     * Gets comma-separated integers, where an empty value is an empty list.
     *
     * @param property the property
     * @return the unmodifiable integers in received order
     * @throws TS3InvalidValueException if the property is missing or an element is not an integer
     */
    protected final List<Integer> requireIntList(Property property) {
        String name = property.getName();
        String value = require(name);
        if (value.isEmpty()) return List.of();
        return requireParsed(
                name,
                "a comma-separated integer list",
                list -> Arrays.stream(list.split(",")).map(Integer::valueOf).toList());
    }

    /**
     * Gets an integer code as an enum constant.
     *
     * @param propertyName the property name
     * @param decoder maps a code to its constant, or to the enum's {@code UNKNOWN} constant
     * @param <E> the enum type
     * @return the decoded constant
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final <E extends Enum<E>> E requireCode(String propertyName, IntFunction<E> decoder) {
        return decoder.apply(requireInt(propertyName));
    }

    /**
     * Gets an integer code as an enum constant.
     *
     * @param property the property
     * @param decoder maps a code to its constant, or to the enum's {@code UNKNOWN} constant
     * @param <E> the enum type
     * @return the decoded constant
     * @throws TS3InvalidValueException if the property is missing or not an integer
     */
    protected final <E extends Enum<E>> E requireCode(Property property, IntFunction<E> decoder) {
        return requireCode(property.getName(), decoder);
    }

    /**
     * Gets a property parsed by the given parser, which must be present.
     *
     * @param propertyName the property name
     * @param expected a description of valid values for error messages, such as {@code "an integer"}
     * @param parser parses a nonempty raw value, throwing {@link IllegalArgumentException} for invalid values
     * @param <T> the parsed type
     * @return the parsed value
     * @throws TS3InvalidValueException if the property is missing, has no value or cannot be parsed
     */
    protected final <T> T requireParsed(String propertyName, String expected, Function<String, T> parser) {
        return findParsed(propertyName, expected, parser)
                .orElseThrow(() -> new TS3InvalidValueException(propertyName, map.get(propertyName), expected, null));
    }

    /**
     * Gets a property parsed by the given parser.
     *
     * @param propertyName the property name
     * @param expected a description of valid values for error messages, such as {@code "an integer"}
     * @param parser parses a nonempty raw value, throwing {@link IllegalArgumentException} for invalid values
     * @param <T> the parsed type
     * @return the parsed value, or empty if the property is missing or has no value
     * @throws TS3InvalidValueException if the value cannot be parsed
     */
    protected final <T> Optional<T> findParsed(String propertyName, String expected, Function<String, T> parser) {
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
    // Different model types are never equal, even with identical properties.
    @SuppressWarnings("EqualsGetClass")
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass() && map.equals(((Wrapper) other).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /** @return the type and properties, with {@linkplain ProtocolRedactor secret} values redacted */
    @Override
    public String toString() {
        var shown = new LinkedHashMap<String, String>();
        map.forEach((key, value) ->
                shown.put(key, ProtocolRedactor.isSecret(key) && !value.isEmpty() ? ProtocolRedactor.REDACTED : value));
        return getClass().getSimpleName() + shown;
    }
}
