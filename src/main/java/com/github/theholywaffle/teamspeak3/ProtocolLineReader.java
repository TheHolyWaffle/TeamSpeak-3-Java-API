package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Retains a partial protocol line across recoverable socket read timeouts. */
final class ProtocolLineReader implements Closeable {
	private final BufferedReader reader;
	private final StringBuilder partialLine = new StringBuilder();

	ProtocolLineReader(InputStream input) {
		reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	String readLine() throws IOException {
		int ch;
		while ((ch = reader.read()) != -1) {
			if (ch == '\n' || ch == '\r') {
				String line = partialLine.toString();
				partialLine.setLength(0);
				return line;
			}
			partialLine.append((char) ch);
		}
		// EOF cannot complete an unterminated protocol line.
		partialLine.setLength(0);
		return null;
	}

	@Override
	public void close() throws IOException { reader.close(); }
}
