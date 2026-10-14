package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Map;
import java.util.OptionalLong;

public class IconFile extends FileListEntry {

    public IconFile(Map<String, String> map) {
        super(map);
    }

    /**
     * Gets the icon ID corresponding to this file.
     *
     * @return this file's icon ID, or empty if the file name does not follow the {@code icon_<crc32>} format
     */
    public OptionalLong getIconId() {
        String fileName = getName();
        if (!fileName.matches("icon_\\d{1,10}")) return OptionalLong.empty();
        long id = Long.parseLong(fileName.substring("icon_".length()));
        // CRC32 IDs are 32 bits
        if ((id & 0xFFFF_FFFF_0000_0000L) != 0) return OptionalLong.empty();
        return OptionalLong.of(id);
    }
}
