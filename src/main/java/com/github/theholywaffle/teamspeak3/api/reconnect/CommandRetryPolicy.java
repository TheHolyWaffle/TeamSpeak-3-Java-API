package com.github.theholywaffle.teamspeak3.api.reconnect;

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

import java.util.Set;

/** Immutable, opt-in replay policy for explicitly selected read-only ServerQuery commands. */
public final class CommandRetryPolicy {
	private static final Set<String> SAFE_READS = Set.of(
		"whoami", "version", "serverinfo", "serverlist", "channellist", "channelinfo", "clientlist", "clientinfo");
	private final int maxRetries;
	private final Set<String> commands;

	private CommandRetryPolicy(int maxRetries, Set<String> commands) {
		this.maxRetries = maxRetries;
		this.commands = commands;
	}

	/** @return a policy that never replays a command with an unknown outcome */
	public static CommandRetryPolicy none() { return new CommandRetryPolicy(0, Set.of()); }

	/**
	 * Selects read commands to replay, in their original position, after session restoration.
	 * Server error responses are never retried. Mutations and context changes are rejected.
	 * @param maxRetries maximum additional sends per command, greater than zero
	 * @param commands exact protocol names from the supported read-only set
	 * @return an immutable policy
	 */
	public static CommandRetryPolicy safeReads(int maxRetries, String... commands) {
		if (maxRetries <= 0) throw new IllegalArgumentException("Retry count must be positive");
		Set<String> selected = Set.of(commands);
		if (selected.isEmpty() || !SAFE_READS.containsAll(selected)) {
			throw new IllegalArgumentException("Select only supported read-only commands: " + SAFE_READS);
		}
		return new CommandRetryPolicy(maxRetries, selected);
	}

	/** @return the maximum number of additional sends */
	public int getMaxRetries() { return maxRetries; }

	/**
	 * @param name protocol command name
	 * @return whether replay is explicitly enabled
	 */
	public boolean allows(String name) { return commands.contains(name); }
}
