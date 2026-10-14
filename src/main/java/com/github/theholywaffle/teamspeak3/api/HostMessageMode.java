package com.github.theholywaffle.teamspeak3.api;

public enum HostMessageMode {
    LOG(1),
    MODAL(2),
    MODAL_QUIT(3),
    UNKNOWN(-1);

    private static final HostMessageMode[] VALUES = values();

    private final int i;

    HostMessageMode(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static HostMessageMode fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, HostMessageMode::getIndex, index, UNKNOWN);
    }
}
