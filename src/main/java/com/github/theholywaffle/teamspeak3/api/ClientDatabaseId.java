package com.github.theholywaffle.teamspeak3.api;

/**
 * The persistent database ID of a client identity ({@code cldbid}), used by groups, permissions,
 * complaints and query logins. It is not the session {@link ClientId} of a connected client.
 *
 * @param value the positive ID
 */
public record ClientDatabaseId(long value) {

    public ClientDatabaseId {
        if (value <= 0) throw new IllegalArgumentException("Client database ID must be positive");
    }

    public static ClientDatabaseId of(long value) {
        return new ClientDatabaseId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
