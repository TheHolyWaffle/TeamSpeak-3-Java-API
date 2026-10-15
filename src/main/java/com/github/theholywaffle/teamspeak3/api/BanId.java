package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a ban rule ({@code banid}), as created by adding a ban or banning clients.
 *
 * @param value the positive ID
 */
public record BanId(long value) {

    public BanId {
        if (value <= 0) throw new IllegalArgumentException("Ban ID must be positive");
    }

    public static BanId of(long value) {
        return new BanId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
