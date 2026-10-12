package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of one connected client session ({@code clid}). It changes on every connection and is
 * not the persistent {@link ClientDatabaseId}. {@code 0} identifies the server as an event invoker.
 *
 * @param value the nonnegative ID
 */
public record ClientId(int value) {

	public ClientId {
		if (value < 0) throw new IllegalArgumentException("Client ID must not be negative");
	}

	public static ClientId of(int value) { return new ClientId(value); }
}
