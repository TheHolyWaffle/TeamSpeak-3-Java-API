package com.github.theholywaffle.teamspeak3.api;

import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionAssignment;

/**
 * An enum describing where a {@link PermissionAssignment} originates from.
 */
public enum PermissionGroupType {

    /**
     * A permission assigned to a server group.
     */
    SERVER_GROUP(0),

    /**
     * A permission assigned to a client on a server-wide level.
     */
    GLOBAL_CLIENT(1),

    /**
     * A permission requirement for certain actions in a channel.
     * <p>
     * Examples: Join, talk, subscribe and file upload permission requirements
     * </p>
     */
    CHANNEL(2),

    /**
     * A permission assigned to a channel group.
     */
    CHANNEL_GROUP(3),

    /**
     * A permission assigned to a client in a specific channel.
     * <p>
     * This is mostly used for the priority speaker feature by granting
     * {@code b_client_is_priority_speaker} to a client in a channel.
     * </p>
     */
    CHANNEL_CLIENT(4),

    /** A code this library does not know; read the raw value from the model's properties. */
    UNKNOWN(-1);

    private static final PermissionGroupType[] VALUES = values();

    private final int i;

    PermissionGroupType(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static PermissionGroupType fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, PermissionGroupType::getIndex, index, UNKNOWN);
    }
}
