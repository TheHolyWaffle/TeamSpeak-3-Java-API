package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a channel group ({@code cgid}), which a client holds per channel. It is not a
 * {@link ServerGroupId}; both kinds of group are numbered independently.
 *
 * @param value the positive ID
 */
public record ChannelGroupId(long value) {

    public ChannelGroupId {
        if (value <= 0) throw new IllegalArgumentException("Channel group ID must be positive");
    }

    public static ChannelGroupId of(long value) {
        return new ChannelGroupId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
