package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;

/**
 * A permission value to assign. Only server-group assignments accept {@link #withNegated()};
 * only server-group and client assignments accept {@link #withSkip()}.
 *
 * @param permissionName the permission name, for example {@code "i_channel_needed_join_power"}
 * @param value the permission value; boolean permissions use 1 or 0
 * @param negated whether the server-group assignment is negated
 * @param skipped whether the assignment is not overwritten by channel group permissions
 */
public record PermissionGrant(String permissionName, int value, boolean negated, boolean skipped) {

	public PermissionGrant {
		Objects.requireNonNull(permissionName, "permissionName");
		if (permissionName.isEmpty()) throw new IllegalArgumentException("Permission name must not be empty");
	}

	public static PermissionGrant of(String permissionName, int value) {
		return new PermissionGrant(permissionName, value, false, false);
	}

	public static PermissionGrant of(IPermissionType permission, int value) {
		return of(permission.getName(), value);
	}

	public static PermissionGrant of(BPermissionType permission, boolean value) {
		return of(permission.getName(), value ? 1 : 0);
	}

	/** @return a negated copy */
	public PermissionGrant withNegated() { return new PermissionGrant(permissionName, value, true, skipped); }

	/** @return a copy that channel group permissions do not overwrite */
	public PermissionGrant withSkip() { return new PermissionGrant(permissionName, value, negated, true); }

	/**
	 * @param target the holder that receives this grant
	 * @throws IllegalArgumentException if the holder does not accept this grant's flags
	 */
	public void requireSupportedBy(PermissionTarget.Kind target) {
		if (negated && !target.supportsNegated()) throw new IllegalArgumentException(target + " permissions cannot be negated");
		if (skipped && !target.supportsSkip()) throw new IllegalArgumentException(target + " permissions cannot be skipped");
	}
}
