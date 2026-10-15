package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a server group ({@code sgid}), as listed in {@code client_servergroups}. It is not a
 * {@link ChannelGroupId}; both kinds of group are numbered independently.
 *
 * @param value the positive ID
 */
public record ServerGroupId(long value) implements EntityId {

    public ServerGroupId {
        if (value <= 0) throw new IllegalArgumentException("Server group ID must be positive");
    }

    public static ServerGroupId of(long value) {
        return new ServerGroupId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
