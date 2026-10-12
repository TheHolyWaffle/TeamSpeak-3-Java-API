package com.github.theholywaffle.teamspeak3.api;

/**
 * The holder of a permission assignment.
 *
 * @param type the type of holder
 * @param id the server group, channel group, channel or client database ID
 * @param clientDatabaseId the client database ID for {@link PermissionGroupType#CHANNEL_CLIENT}, otherwise 0
 */
public record PermissionTarget(PermissionGroupType type, int id, int clientDatabaseId) {

	public PermissionTarget {
		if (type == null) throw new IllegalArgumentException("Type must not be null");
		if (id <= 0) throw new IllegalArgumentException("ID must be positive");
		if ((type == PermissionGroupType.CHANNEL_CLIENT) != (clientDatabaseId > 0)) {
			throw new IllegalArgumentException("Only channel-client targets use a positive client database ID");
		}
	}

	public static PermissionTarget serverGroup(int serverGroupId) { return new PermissionTarget(PermissionGroupType.SERVER_GROUP, serverGroupId, 0); }

	public static PermissionTarget channelGroup(int channelGroupId) { return new PermissionTarget(PermissionGroupType.CHANNEL_GROUP, channelGroupId, 0); }

	public static PermissionTarget channel(int channelId) { return new PermissionTarget(PermissionGroupType.CHANNEL, channelId, 0); }

	public static PermissionTarget client(int clientDatabaseId) { return new PermissionTarget(PermissionGroupType.GLOBAL_CLIENT, clientDatabaseId, 0); }

	public static PermissionTarget channelClient(int channelId, int clientDatabaseId) {
		return new PermissionTarget(PermissionGroupType.CHANNEL_CLIENT, channelId, clientDatabaseId);
	}
}
