package com.github.theholywaffle.teamspeak3.api;

public enum HostBannerMode {
    NO_ADJUST(0),
    IGNORE_ASPECT(1),
    KEEP_ASPECT(2),
    UNKNOWN(-1);

    private static final HostBannerMode[] VALUES = values();

    private final int i;

    HostBannerMode(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static HostBannerMode fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, HostBannerMode::getIndex, index, UNKNOWN);
    }
}
