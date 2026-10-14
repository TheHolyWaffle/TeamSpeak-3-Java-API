package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;

/**
 * A file or directory path within a channel's file repository. Channel ID 0 addresses the
 * virtual server's own files, such as icons and avatars.
 *
 * @param channelId the channel owning the file, or 0 for server files
 * @param path the absolute path, for example {@code "/docs/readme.txt"}
 * @param channelPassword the channel password, or {@code null} if none is required
 */
public record FileLocation(int channelId, String path, String channelPassword) {

    public FileLocation {
        if (channelId < 0) throw new IllegalArgumentException("Channel ID must not be negative");
        Objects.requireNonNull(path, "path");
        if (path.isEmpty()) throw new IllegalArgumentException("Path must not be empty");
    }

    /**
     * @param channelId the channel owning the file, or 0 for server files
     * @param path the absolute path
     * @return a location that needs no channel password
     */
    public static FileLocation of(int channelId, String path) {
        return new FileLocation(channelId, path, null);
    }

    /**
     * @param password the channel password, or {@code null} if none is required
     * @return a copy using the given channel password
     */
    public FileLocation withChannelPassword(String password) {
        return new FileLocation(channelId, path, password);
    }

    /**
     * @param newPath the absolute path in the same channel
     * @return a copy addressing another path with the same channel and password
     */
    public FileLocation withPath(String newPath) {
        return new FileLocation(channelId, newPath, channelPassword);
    }
}
