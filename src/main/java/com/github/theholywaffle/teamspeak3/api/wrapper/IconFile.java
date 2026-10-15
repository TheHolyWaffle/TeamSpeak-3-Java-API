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
        return IconId.fromFileName(getName());
    }
}
