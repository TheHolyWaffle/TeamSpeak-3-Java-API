package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;

/**
 * A {@code -option} flag.
 * @param option unencoded option name
 */
public record OptionParam(String option) implements Parameter {

    @Override
    public void addTo(ParameterSink sink) {
        sink.put("-" + option, "");
    }

    @Override
    public void appendTo(StringBuilder line) {
        CommandEncoding.encode(option, line.append('-'));
    }
}
