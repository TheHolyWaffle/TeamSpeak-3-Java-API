package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.commands.CommandEncoding;

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

/**
 * Text sent unchanged.
 * @param raw already encoded text
 */
public record RawParam(String raw) implements Parameter {

    /** Decodes the already encoded text: entries split at pipes, parameters at spaces. */
    @Override
    public void addTo(ParameterSink sink) {
        String[] entries = raw.split("\\|", -1);
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) sink.nextEntry();
            for (String token : entries[i].split(" ", -1)) {
                if (token.isEmpty()) continue;
                int equals = token.indexOf('=');
                sink.put(
                        CommandEncoding.decode(equals < 0 ? token : token.substring(0, equals)),
                        equals < 0 ? "" : CommandEncoding.decode(token.substring(equals + 1)));
            }
        }
    }

    @Override
    public void appendTo(StringBuilder line) {
        line.append(raw);
    }
}
