package com.github.theholywaffle.teamspeak3.api;

/** What a Web Query API key may do; each scope includes the ones below it. */
public enum ApiKeyScope {
    /** Administration, including API key management. */
    MANAGE("manage"),
    /** Commands that change server state. */
    WRITE("write"),
    /** Commands that only read server state. */
    READ("read"),
    /** A scope this library does not know; never sent. */
    UNKNOWN("unknown");

    private static final ApiKeyScope[] VALUES = values();

    private final String name;

    ApiKeyScope(String name) {
        this.name = name;
    }

    /** @return the ServerQuery scope name */
    public String getName() {
        return name;
    }

    /**
     * @param name the ServerQuery scope name
     * @return the matching constant, or {@link #UNKNOWN} for a scope this library does not know
     */
    public static ApiKeyScope fromName(String name) {
        for (ApiKeyScope value : VALUES) {
            if (value != UNKNOWN && value.name.equals(name)) return value;
        }
        return UNKNOWN;
    }
}
