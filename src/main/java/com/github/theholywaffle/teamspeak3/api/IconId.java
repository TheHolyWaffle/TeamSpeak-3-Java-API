package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID of an uploaded icon: the CRC32 of its content, stored as the file {@code /icon_<id>}.
 * TeamSpeak sends it unsigned in channel, client and virtual server properties, but as a signed 32-bit
 * value in group lists and in the {@code i_icon_id} permission. {@code 0} means no icon.
 *
 * @param value the unsigned ID, from 1 to 2<sup>32</sup>-1
 */
public record IconId(long value) {

    public IconId {
        if (value <= 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("Icon ID must be between 1 and 4294967295: " + value);
        }
    }

    public static IconId of(long value) {
        return new IconId(value);
    }

    /**
     * @param value a nonzero {@code i_icon_id} permission value
     * @return the icon ID
     */
    public static IconId fromPermissionValue(int value) {
        return new IconId(Integer.toUnsignedLong(value));
    }

    /** @return the signed 32-bit value of the {@code i_icon_id} permission */
    public int permissionValue() {
        return (int) value;
    }

    /** @return the unsigned number, as used in the icon's file name */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
