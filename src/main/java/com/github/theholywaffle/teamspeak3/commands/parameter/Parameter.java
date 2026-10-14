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

/** One part of a ServerQuery command line. */
public sealed interface Parameter permits ArrayParameter, KeyValueParam, OptionParam, RawParam, ValueParam {

    /**
     * Appends this parameter in ServerQuery syntax, with values escaped.
     * @param line the command line being built
     */
    void appendTo(StringBuilder line);

    /**
     * Passes this parameter to {@code sink} without ServerQuery escaping.
     * @param sink the receiver of names, values and entry boundaries
     */
    void addTo(ParameterSink sink);

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
