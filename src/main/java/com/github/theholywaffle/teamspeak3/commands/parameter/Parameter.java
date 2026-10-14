package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;
import java.util.List;
import java.util.Map;

/** One part of a ServerQuery command line. */
public sealed interface Parameter permits ArrayParameter, KeyValueParam, OptionParam, RawParam, ValueParam {

    /**
     * Appends this parameter in ServerQuery syntax, with values escaped.
     * @param line the command line being built
     */
    void appendTo(StringBuilder line);

    /**
     * Passes this parameter to {@code sink} without ServerQuery escaping. The default decodes
     * {@link #encoded()}: entries split at pipes, parameters at spaces.
     * @param sink the receiver of names, values and entry boundaries
     */
    default void addTo(ParameterSink sink) {
        List<Map<String, String>> entries = CommandEncoding.decodeEntries(encoded());
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sink.nextEntry();
            entries.get(i).forEach(sink::put);
        }
    }

    /**
     * Encodes this parameter on its own.
     * @return this parameter in ServerQuery syntax, with values escaped
     */
    default String encoded() {
        var line = new StringBuilder();
        appendTo(line);
        return line.toString();
    }
}
