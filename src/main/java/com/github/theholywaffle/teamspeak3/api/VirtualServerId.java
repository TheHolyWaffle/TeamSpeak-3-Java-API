package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a virtual server within a server instance ({@code sid}), not its voice port.
 * TeamSpeak sends {@code 0} for the instance itself or when no virtual server is selected; those values
 * are absent rather than a virtual server ID.
 *
 * @param value the positive ID
 */
public record VirtualServerId(long value) {

    public VirtualServerId {
        if (value <= 0) throw new IllegalArgumentException("Virtual server ID must be positive");
    }

    public static VirtualServerId of(long value) {
        return new VirtualServerId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
