package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Objects;

/**
 * A permission assigned to a server group, channel group, channel or client.
 * <p>
 * The value is the assigned value, not necessarily the client's effective value, which other
 * assignments may override. Boolean permissions use {@code 0} and {@code 1}. Negated assignments
 * select the lowest instead of the highest value. Skipped assignments (server groups and clients only)
 * are not overridden by channel group or, for server groups, client permissions.
 * </p>
 *
 * @param name the permission name; {@code b_} for boolean and {@code i_} for integer permissions
 * @param value the assigned value
 * @param negated whether the assignment is negated
 * @param skipped whether the assignment is skipped
 */
public record Permission(String name, int value, boolean negated, boolean skipped) {

    public Permission {
        Objects.requireNonNull(name, "name");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static Permission from(Wrapper row) {
        return new Permission(
                row.require("permsid"),
                row.requireInt("permvalue"),
                row.requireBoolean("permnegated"),
                row.requireBoolean("permskip"));
    }
}
