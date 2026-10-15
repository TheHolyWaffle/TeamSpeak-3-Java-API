package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.IconId;
import java.util.Map;
import java.util.Optional;

public class IconFile extends FileListEntry {

    public IconFile(Map<String, String> map) {
        super(map);
    }

    /**
     * Gets the icon ID corresponding to this file.
     *
     * @return this file's icon ID, or empty if the file name is not {@code icon_<id>} with a valid ID
     */
    public Optional<IconId> getIconId() {
        String fileName = getName();
        if (!fileName.matches("icon_\\d{1,10}")) return Optional.empty();
        long id = Long.parseLong(fileName.substring("icon_".length()));
        if (id == 0 || id > 0xFFFF_FFFFL) return Optional.empty();
        return Optional.of(IconId.of(id));
    }
}
