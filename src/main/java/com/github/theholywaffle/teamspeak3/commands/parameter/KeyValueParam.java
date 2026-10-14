package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;

/**
 * A {@code key=value} pair.
 * @param key unencoded key
 * @param value unencoded value; {@code null} is sent as an empty value
 */
public record KeyValueParam(String key, String value) implements Parameter {

    public KeyValueParam {
        if (key == null) throw new IllegalArgumentException("Key was null");
        if (value == null) value = "";
    }

    public KeyValueParam(String key, int value) {
        this(key, String.valueOf(value));
    }

    public KeyValueParam(String key, long value) {
        this(key, String.valueOf(value));
    }

    public KeyValueParam(String key, boolean value) {
        this(key, value ? "1" : "0");
    }

    @Override
    public void addTo(ParameterSink sink) {
        sink.put(key, value);
    }

    @Override
    public void appendTo(StringBuilder line) {
        CommandEncoding.encode(value, CommandEncoding.encode(key, line).append('='));
    }
}
