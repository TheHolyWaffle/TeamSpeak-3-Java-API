package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;

/**
 * A value without a key, such as the {@code login} credentials.
 * @param value unencoded value
 */
public record ValueParam(String value) implements Parameter {

    @Override
    public void addTo(ParameterSink sink) {
        sink.put(value, "");
    }

    @Override
    public void appendTo(StringBuilder line) {
        CommandEncoding.encode(value, line);
    }
}
