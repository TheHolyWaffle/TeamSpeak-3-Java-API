package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.api.EntityId;
import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import java.util.Optional;

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

    public KeyValueParam(String key, EntityId id) {
        this(key, id.value());
    }

    /**
     * @param key unencoded key
     * @param id the ID, or empty where the command expects {@code 0} for none
     * @return the pair, with value {@code 0} for an empty ID
     */
    public static KeyValueParam idOrZero(String key, Optional<? extends EntityId> id) {
        return new KeyValueParam(key, id.map(EntityId::value).orElse(0L));
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
