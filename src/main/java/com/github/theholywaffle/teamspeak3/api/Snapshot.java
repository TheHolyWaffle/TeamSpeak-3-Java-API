package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;

/**
 * An encoded virtual server snapshot, as returned by {@code serversnapshotcreate}.
 *
 * @param data the encoded snapshot, passed back unchanged when deploying
 */
public record Snapshot(String data) {

    public Snapshot {
        Objects.requireNonNull(data, "data");
    }

    /** Omits the data, which contains server configuration such as password hashes. */
    @Override
    public String toString() {
        return "Snapshot[" + data.length() + " characters]";
    }
}
