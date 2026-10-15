package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of an offline message in the query's own mailbox ({@code msgid}).
 *
 * @param value the positive ID
 */
public record MessageId(long value) {

    public MessageId {
        if (value <= 0) throw new IllegalArgumentException("Message ID must be positive");
    }

    public static MessageId of(long value) {
        return new MessageId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
