package com.github.theholywaffle.teamspeak3.api;

public enum PrivilegeKeyType {
    SERVER_GROUP(0),
    CHANNEL_GROUP(1),

    /** A code this library does not know; read the raw value from the model's properties. */
    UNKNOWN(-1);

    private static final PrivilegeKeyType[] VALUES = values();

    private final int i;

    PrivilegeKeyType(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static PrivilegeKeyType fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, PrivilegeKeyType::getIndex, index, UNKNOWN);
    }
}
