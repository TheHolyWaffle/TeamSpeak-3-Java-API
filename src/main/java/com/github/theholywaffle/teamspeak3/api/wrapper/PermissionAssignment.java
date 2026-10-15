package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Map;

/**
 * A permission assignment with its value and flags, as returned by {@code permoverview}.
 */
public class PermissionAssignment extends PermissionSource {

    public PermissionAssignment(Map<String, String> map) {
        super(map);
    }

    /**
     * Gets the value of this permission assignment.
     * <p>
     * Please note that this value doesn't necessarily have to be
     * the effective permission value for a client, as this assignment
     * can be overridden by another assignment.
     * </p><p>
     * Integer permissions usually have values between 0 and 100,
     * but any integer value is theoretically valid.
     * </p><p>
     * Boolean permissions have a value of {@code 0} to represent
     * {@code false} and {@code 1} to represent {@code true}.
     * </p>
     *
     * @return the value of this permission assignment
     */
    public int getValue() {
        return requireInt("v");
    }

    /**
     * Returns {@code true} if this permission is negated.
     * <p>
     * Negated means that instead of the highest value, the lowest
     * value will be selected for this permission instead.
     * </p>
     *
     * @return whether this permission is negated
     */
    public boolean isNegated() {
        return requireBoolean("n");
    }

    /**
     * Returns {@code true} if this permission is skipped.
     * <p>
     * Skipped only exists for server group and client permissions,
     * therefore this value will always be false for channel group permissions.
     * </p><p>
     * If a client permission is skipped, it won't be overridden by channel
     * group permissions.<br>
     * If a server group permission is skipped, it won't be overridden by
     * channel group or client permissions.
     * </p>
     *
     * @return whether this permission is skipped
     */
    public boolean isSkipped() {
        return requireBoolean("s");
    }
}
