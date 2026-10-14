package com.github.theholywaffle.teamspeak3.api;

public enum TextMessageTargetMode {
    CLIENT(1),
    CHANNEL(2),
    SERVER(3),

    /** A code this library does not know; read the raw value from the model's properties. */
    UNKNOWN(-1);

    private static final TextMessageTargetMode[] VALUES = values();

    private final int i;

    TextMessageTargetMode(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static TextMessageTargetMode fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, TextMessageTargetMode::getIndex, index, UNKNOWN);
    }
}
