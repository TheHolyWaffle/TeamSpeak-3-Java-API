package com.github.theholywaffle.teamspeak3.commands;

final class KnownCodes {

    private KnownCodes() {}

    /** Rejects the {@code UNKNOWN} response-only constants, whose index is {@code -1}. */
    static int require(int index) {
        if (index < 0)
            throw new IllegalArgumentException("UNKNOWN is only used for received values and cannot be sent");
        return index;
    }
}
