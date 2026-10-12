package com.github.theholywaffle.teamspeak3.api;

/**
 * The holder of a permission assignment.
 *
 * @param kind the type of holder
 * @param id the server group, channel group, channel or client database ID
 * @param clientDatabaseId the client database ID for {@link Kind#CHANNEL_CLIENT}, otherwise 0
 */
public record PermissionTarget(Kind kind, int id, int clientDatabaseId) {

	/** Types of permission holders. */
	public enum Kind {
		SERVER_GROUP(true, true),
		CHANNEL_GROUP(false, false),
		CHANNEL(false, false),
		CLIENT(false, true),
		CHANNEL_CLIENT(false, false);

		private final boolean negatable;
		private final boolean skippable;

		Kind(boolean negatable, boolean skippable) {
			this.negatable = negatable;
			this.skippable = skippable;
		}

		/** @return whether assignments to this holder accept the negated flag */
		public boolean supportsNegated() { return negatable; }

		/** @return whether assignments to this holder accept the skip flag */
		public boolean supportsSkip() { return skippable; }
	}

	public PermissionTarget {
		if (kind == null) throw new IllegalArgumentException("Kind must not be null");
		if (id <= 0) throw new IllegalArgumentException("ID must be positive");
		if ((kind == Kind.CHANNEL_CLIENT) != (clientDatabaseId > 0)) {
			throw new IllegalArgumentException("Only channel-client targets use a positive client database ID");
		}
	}

	public static PermissionTarget serverGroup(int serverGroupId) { return new PermissionTarget(Kind.SERVER_GROUP, serverGroupId, 0); }

	public static PermissionTarget channelGroup(int channelGroupId) { return new PermissionTarget(Kind.CHANNEL_GROUP, channelGroupId, 0); }

	public static PermissionTarget channel(int channelId) { return new PermissionTarget(Kind.CHANNEL, channelId, 0); }

	public static PermissionTarget client(int clientDatabaseId) { return new PermissionTarget(Kind.CLIENT, clientDatabaseId, 0); }

	public static PermissionTarget channelClient(int channelId, int clientDatabaseId) {
		return new PermissionTarget(Kind.CHANNEL_CLIENT, channelId, clientDatabaseId);
	}
}
