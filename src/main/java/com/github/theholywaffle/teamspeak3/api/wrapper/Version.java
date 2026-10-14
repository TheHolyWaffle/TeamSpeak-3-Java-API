package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Objects;

/**
 * The server version from {@code version}.
 *
 * @param version the version number, such as {@code 3.13.8}
 * @param build the build identifier, such as {@code 1779874471}
 * @param platform the server platform, such as {@code Linux}
 */
public record Version(String version, String build, String platform) {

    public Version {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(platform, "platform");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing */
    public static Version from(Wrapper row) {
        return new Version(row.require("version"), row.require("build"), row.require("platform"));
    }
}
