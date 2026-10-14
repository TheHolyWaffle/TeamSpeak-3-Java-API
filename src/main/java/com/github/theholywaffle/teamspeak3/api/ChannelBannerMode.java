package com.github.theholywaffle.teamspeak3.api;

/**
 * The values in this enum are <b>speculative</b>.
 * There does not appear to be any documentation on channel banner modes.
 */
public enum ChannelBannerMode {
    NO_ADJUST(0),
    IGNORE_ASPECT(1),
    KEEP_ASPECT(2),

    /**
     * An unknown channel banner mode.
     * <p>
     * If you ever encounter an unknown banner mode, please tell us!
     * We may have to update the API.
     * </p>
     */
    UNKNOWN(-1);

    private static final ChannelBannerMode[] VALUES = values();

    private final int i;

    ChannelBannerMode(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static ChannelBannerMode fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, ChannelBannerMode::getIndex, index, UNKNOWN);
    }
}
