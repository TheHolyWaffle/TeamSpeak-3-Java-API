package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of a Web Query API key ({@code id} in {@code apikeylist}), not its secret.
 *
 * @param value the positive ID
 */
public record ApiKeyId(long value) implements EntityId {

    public ApiKeyId {
        if (value <= 0) throw new IllegalArgumentException("API key ID must be positive");
    }

    public static ApiKeyId of(long value) {
        return new ApiKeyId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
