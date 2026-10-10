package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2019 Bert De Geyter, Roger Baumgartner
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** A fresh ordered UTF-8 ServerQuery byte stream for one connection.
 * Implementations own only transport I/O: framing, commands and events are shared by TS3Query.
 * close must abort concurrent connect/read/write promptly, including partially initialized resources.
 */
public interface QueryTransport extends Closeable {
	/** Callback marking the transition from connect to handshake deadline. */
	@FunctionalInterface interface Connected {
		/** @throws IOException when connection initialization was cancelled */
		void transportConnected() throws IOException;
	}

	/** @param connection call once after connect, before handshake/authentication
	 * @throws IOException if transport initialization fails */
	void connect(Connected connection) throws IOException;

	/** @param timeout remaining shutdown budget for owned workers */
	default void awaitTermination(java.time.Duration timeout) { }

	/** @return whether every owned worker has stopped */
	default boolean isTerminated() { return true; }

	/** @return ordered query input @throws IOException if unavailable */
	InputStream getInputStream() throws IOException;

	/** @return ordered query output @throws IOException if unavailable */
	OutputStream getOutputStream() throws IOException;
}
