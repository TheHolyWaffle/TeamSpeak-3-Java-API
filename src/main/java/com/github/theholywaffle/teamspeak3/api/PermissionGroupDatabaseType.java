package com.github.theholywaffle.teamspeak3.api;

public enum PermissionGroupDatabaseType {
    TEMPLATE(0),
    REGULAR(1),
    QUERY(2),

    /** A code this library does not know; read the raw value from the model's properties. */
    UNKNOWN(-1);

    private static final PermissionGroupDatabaseType[] VALUES = values();

    private final int i;

    PermissionGroupDatabaseType(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static PermissionGroupDatabaseType fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, PermissionGroupDatabaseType::getIndex, index, UNKNOWN);
    }
}
