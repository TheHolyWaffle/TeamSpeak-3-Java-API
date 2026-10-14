package com.github.theholywaffle.teamspeak3.commands.parameter;

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
