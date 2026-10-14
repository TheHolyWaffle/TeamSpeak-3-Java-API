package com.github.theholywaffle.teamspeak3.commands.parameter;

import java.util.Arrays;
import java.util.List;

/**
 * Pipe-separated entries, each made of space-separated parameters.
 * @param entries the entries; copied
 */
public record ArrayParameter(List<List<Parameter>> entries) implements Parameter {

    public ArrayParameter {
        entries = entries.stream().map(List::copyOf).toList();
    }

    /**
     * Creates one {@code key=value} entry per value.
     * @param key unencoded key
     * @param values unencoded values
     * @return the array parameter
     */
    public static ArrayParameter of(String key, List<String> values) {
        return new ArrayParameter(values.stream()
                .map(value -> List.<Parameter>of(new KeyValueParam(key, value)))
                .toList());
    }

    /**
     * Creates one {@code key=value} entry per value.
     * @param key unencoded key
     * @param values numeric values
     * @return the array parameter
     */
    public static ArrayParameter of(String key, int[] values) {
        return of(key, Arrays.stream(values).mapToObj(String::valueOf).toList());
    }

    @Override
    public void addTo(ParameterSink sink) {
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sink.nextEntry();
            for (Parameter parameter : entries.get(i)) parameter.addTo(sink);
        }
    }

    @Override
    public void appendTo(StringBuilder line) {
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) line.append('|');
            List<Parameter> entry = entries.get(i);
            for (int j = 0; j < entry.size(); j++) {
                if (j > 0) line.append(' ');
                entry.get(j).appendTo(line);
            }
        }
    }
}
