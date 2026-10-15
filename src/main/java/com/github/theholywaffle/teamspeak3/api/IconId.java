package com.github.theholywaffle.teamspeak3.api;

import java.util.Optional;

/**
 * The ID of an uploaded icon: the CRC32 of its content, stored as the file {@code /icon_<id>}.
 * TeamSpeak sends it unsigned in channel, client and virtual server properties, but as a signed 32-bit
 * value in group lists and in the {@code i_icon_id} permission. {@code 0} means no icon.
 *
 * @param value the unsigned ID, from 1 to 2<sup>32</sup>-1
 */
public record IconId(long value) implements EntityId {

    private static final long MAX = 0xFFFF_FFFFL;
    private static final String FILE_PREFIX = "icon_";

    public IconId {
        if (value <= 0 || value > MAX) {
            throw new IllegalArgumentException("Icon ID must be between 1 and 4294967295: " + value);
        }
    }

    public static IconId of(long value) {
        return new IconId(value);
    }

    /**
     * @param value an icon ID as TeamSpeak sends it, signed or unsigned
     * @return the icon ID, or empty for {@code 0}
     * @throws IllegalArgumentException if the value does not fit in 32 bits
     */
    public static Optional<IconId> fromWire(long value) {
        if (value < Integer.MIN_VALUE || value > MAX) {
            throw new IllegalArgumentException("Icon ID must fit in 32 bits: " + value);
        }
        long unsigned = value & MAX;
        return unsigned == 0 ? Optional.empty() : Optional.of(new IconId(unsigned));
    }

    /**
     * @param fileName a file name such as {@code icon_123}
     * @return the icon ID, or empty if the name is not {@code icon_<id>} with a valid ID
     */
    public static Optional<IconId> fromFileName(String fileName) {
        if (!fileName.matches(FILE_PREFIX + "\\d{1,10}")) return Optional.empty();
        long id = Long.parseLong(fileName.substring(FILE_PREFIX.length()));
        return id > MAX ? Optional.empty() : fromWire(id);
    }

    /**
     * @param value a nonzero {@code i_icon_id} permission value
     * @return the icon ID
     * @throws IllegalArgumentException if the value is {@code 0}
     */
    public static IconId fromPermissionValue(int value) {
        return fromWire(value).orElseThrow(() -> new IllegalArgumentException("Icon ID must not be 0"));
    }

    /** @return the signed 32-bit value of the {@code i_icon_id} permission */
    public int permissionValue() {
        return (int) value;
    }

    /** @return the icon's path among the virtual server's files, {@code /icon_<id>} */
    public String path() {
        return "/" + FILE_PREFIX + value;
    }

    /** @return the unsigned number, as used in the icon's file name */
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
