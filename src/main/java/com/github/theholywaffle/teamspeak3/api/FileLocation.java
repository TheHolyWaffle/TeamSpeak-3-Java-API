package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;
import java.util.Optional;

/**
 * A file or directory path within a channel's file repository, or within the virtual server's own
 * files, such as icons and avatars.
 *
 * @param channelId the channel owning the file, or empty for server files
 * @param path the absolute path, for example {@code "/docs/readme.txt"}
 * @param channelPassword the channel password, or empty if none is required
 */
public record FileLocation(Optional<ChannelId> channelId, String path, Optional<String> channelPassword) {

    public FileLocation {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(channelPassword, "channelPassword");
        if (path.isEmpty()) throw new IllegalArgumentException("Path must not be empty");
    }

    /**
     * @param channelId the channel owning the file
     * @param path the absolute path
     * @return a location that needs no channel password
     */
    public static FileLocation of(ChannelId channelId, String path) {
        return new FileLocation(Optional.of(channelId), path, Optional.empty());
    }

    /**
     * @param path the absolute path among the virtual server's own files, for example {@code "/icons/"}
     * @return a location outside any channel
     */
    public static FileLocation ofServer(String path) {
        return new FileLocation(Optional.empty(), path, Optional.empty());
    }

    /**
     * @param password the channel password
     * @return a copy using the given channel password
     */
    public FileLocation withChannelPassword(String password) {
        return new FileLocation(channelId, path, Optional.of(password));
    }

    /**
     * @param newPath the absolute path in the same channel
     * @return a copy addressing another path with the same channel and password
     */
    public FileLocation withPath(String newPath) {
        return new FileLocation(channelId, newPath, channelPassword);
    }
}
