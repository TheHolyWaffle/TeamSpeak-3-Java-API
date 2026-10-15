package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a channel on one virtual server ({@code cid}). It is only unique within its virtual server.
 * TeamSpeak sends {@code 0} where there is no channel, such as the parent of a top-level channel or the
 * channel of a client that is leaving the server; those values are absent rather than a channel ID.
 *
 * @param value the positive ID
 */
public record ChannelId(long value) {

    public ChannelId {
        if (value <= 0) throw new IllegalArgumentException("Channel ID must be positive");
    }

    public static ChannelId of(long value) {
        return new ChannelId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
