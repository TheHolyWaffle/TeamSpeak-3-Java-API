package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class FileInfo extends Wrapper {

    public FileInfo(Map<String, String> map) {
        super(map);
    }

    /**
     * Gets the ID of the channel this file is stored on.
     *
     * @return the ID of the file's channel, or empty for the virtual server's own files, such as icons
     */
    public Optional<ChannelId> getChannelId() {
        return requireOptionalId("cid", ChannelId::of);
    }

    /**
     * Gets the path to this file or directory, including its name.
     * This path can be used in other file transfer commands.
     *
     * @return the path to the file or directory
     */
    public String getPath() {
        return require("name");
    }

    /**
     * Gets the name of this file or directory.
     *
     * @return the name of this file or directory
     */
    public String getName() {
        String fullPath = getPath();
        int slashPos = fullPath.lastIndexOf('/');
        if (slashPos < 0) return fullPath;
        return fullPath.substring(slashPos + 1);
    }

    /**
     * Gets the path of the directory containing this file or directory.
     *
     * @return the path to the parent directory
     *
     * @see #getPath()
     */
    public String getParentPath() {
        String fullPath = getPath();
        int slashPos = fullPath.lastIndexOf('/');
        if (slashPos < 0) return "/";
        return fullPath.substring(0, slashPos + 1);
    }

    /**
     * Gets the size of the file in bytes.
     * Note that this can return wrong values if the file is still being uploaded
     * or if the upload has been paused by the uploading client.
     *
     * @return the file's size
     */
    public long getFileSize() {
        return requireLong("size");
    }

    /**
     * Gets the date of the last modification to this file.
     * Interprets the server's {@code datetime} field as epoch seconds, as supplied
     * by TS3.
     *
     * @return the file's last modification date, interpreting the field as seconds
     */
    public Instant getLastModifiedDate() {
        return requireInstant("datetime");
    }

    /**
     * Gets whether this entry is a directory or a file.
     * {@code 0} stands for a directory, {@code 1} for a file.
     * <p>
     * Consider using {@link #isFile} and {@link #isDirectory} instead.
     * </p>
     *
     * @return the type of this file entry
     */
    public int getType() {
        return 1;
    }

    /**
     * Returns {@code true} if this entry is a file and not a directory.
     *
     * @return whether this is a file
     */
    public boolean isFile() {
        return true;
    }

    /**
     * Returns {@code true} if this entry is a directory and not a file.
     *
     * @return whether this is a directory
     */
    public boolean isDirectory() {
        return false;
    }
}
