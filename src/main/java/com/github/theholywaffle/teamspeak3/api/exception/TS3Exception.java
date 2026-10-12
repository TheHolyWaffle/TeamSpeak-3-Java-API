package com.github.theholywaffle.teamspeak3.api.exception;

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
 * Base type of every failure reported by this library.
 * <p>
 * Subtypes identify the category: {@link TS3CommandFailedException} (server error response),
 * {@link TS3TransportException} (connection unavailable), {@link TS3TimeoutException} (deadline expired),
 * {@link TS3QueueFullException} (admission rejected), {@link TS3UnsupportedOperationException}
 * and {@link TS3FileTransferFailedException}. Use {@link #isOutcomeUnknown()} rather than a type
 * check to decide whether a failed command may still have executed.
 * </p>
 */
public class TS3Exception extends RuntimeException {

	private static final long serialVersionUID = 7167169981592989359L;

	public TS3Exception(String msg) {
		super(msg);
	}

	public TS3Exception(String msg, Throwable cause) {
		super(msg, cause);
	}

	/** Whether this failure represents the given ServerQuery error ID. */
	public boolean matchesQueryError(int errorId) { return false; }

	/**
	 * Whether the server may have executed the failed ServerQuery command. Only retry such a command
	 * when repeating it is safe. File transfer failures describe the separate file socket; see
	 * {@link TS3FileTransferFailedException}.
	 *
	 * @return {@code true} if the command was written but its response never arrived
	 */
	public boolean isOutcomeUnknown() { return false; }

}
