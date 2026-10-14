package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Objects;

/**
 * A permission known to the connected server, from {@code permissionlist}. Prefer the name to the
 * numerical ID, which is specific to the server.
 *
 * @param id the server's numerical permission ID
 * @param name the permission name
 * @param description a short description, which may be empty
 */
public record PermissionInfo(int id, String name, String description) {

    public PermissionInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static PermissionInfo from(Wrapper row) {
        return new PermissionInfo(row.requireInt("permid"), row.require("permname"), row.require("permdesc"));
    }
}
