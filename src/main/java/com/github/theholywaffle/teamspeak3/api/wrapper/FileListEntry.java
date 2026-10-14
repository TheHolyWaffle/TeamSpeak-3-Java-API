package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Map;
import java.util.OptionalLong;

public class FileListEntry extends FileInfo {

    public FileListEntry(Map<String, String> map) {
        super(map);
    }

    @Override
    public String getPath() {
        return getParentPath() + getName();
    }

    @Override
    public String getName() {
        return require("name");
    }

    @Override
    public String getParentPath() {
        return require("path");
    }

    @Override
    public long getFileSize() {
        // Present if still uploading and returns
        return incompleteSize().orElseGet(super::getFileSize);
    }

    @Override
    public int getType() {
        return requireInt("type");
    }

    @Override
    public boolean isFile() {
        return getType() == 1;
    }

    @Override
    public boolean isDirectory() {
        return getType() == 0;
    }

    /**
     * Returns {@code true} if this file was actively being uploaded at the time
     * this object was created. Note that this will return {@code false} if a
     * client has paused an upload.
     *
     * @return whether this file is actively being uploaded
     */
    public boolean isStillUploading() {
        return incompleteSize().isPresent();
    }

    /**
     * If this file is still uploading, this method will return how many bytes have already
     * been uploaded. Otherwise the normal file size is returned.
     *
     * @return how many bytes of this file have been uploaded
     */
    public long getUploadedBytes() {
        return super.getFileSize();
    }

    /** TeamSpeak only sends {@code incompletesize} for files that are still being uploaded. */
    private OptionalLong incompleteSize() {
        OptionalLong size = findLong("incompletesize");
        return size.isPresent() && size.getAsLong() > 0 ? size : OptionalLong.empty();
    }
}
