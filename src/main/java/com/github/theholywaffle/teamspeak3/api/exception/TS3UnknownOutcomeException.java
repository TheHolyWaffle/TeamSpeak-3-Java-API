package com.github.theholywaffle.teamspeak3.api.exception;

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

/** The transport ended after a command entered the write phase; the server may have applied it. */
public class TS3UnknownOutcomeException extends TS3QueryShutDownException {
	private static final long serialVersionUID = 1L;
	private final String commandName;

	/** @param commandName protocol command name, excluding parameters and credentials */
	public TS3UnknownOutcomeException(String commandName) { this.commandName = commandName; }

	/** @return the command name whose outcome is unknown */
	public String getCommandName() { return commandName; }

	@Override
	public String getMessage() { return "Outcome of " + commandName + " is unknown; it may have executed on the server"; }
}
