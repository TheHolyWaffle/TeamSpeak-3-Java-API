package com.github.theholywaffle.teamspeak3.api;

/**
 * The holder of a permission assignment. Create it with the factories, which take typed IDs.
 *
 * @param type the type of holder
 * @param id the server group, channel group, channel or client database ID
 * @param clientDatabaseId the client database ID for {@link PermissionGroupType#CHANNEL_CLIENT}, otherwise 0
 */
public record PermissionTarget(PermissionGroupType type, long id, long clientDatabaseId) {

    public PermissionTarget {
        if (type == null || type == PermissionGroupType.UNKNOWN)
            throw new IllegalArgumentException("Type must be a known holder type");
        if (id <= 0) throw new IllegalArgumentException("ID must be positive");
        if ((type == PermissionGroupType.CHANNEL_CLIENT) != (clientDatabaseId > 0)) {
            throw new IllegalArgumentException("Only channel-client targets use a positive client database ID");
        }
    }

    public static PermissionTarget serverGroup(ServerGroupId serverGroupId) {
        return new PermissionTarget(PermissionGroupType.SERVER_GROUP, serverGroupId.value(), 0);
    }

    public static PermissionTarget channelGroup(ChannelGroupId channelGroupId) {
        return new PermissionTarget(PermissionGroupType.CHANNEL_GROUP, channelGroupId.value(), 0);
    }

    public static PermissionTarget channel(ChannelId channelId) {
        return new PermissionTarget(PermissionGroupType.CHANNEL, channelId.value(), 0);
    }

    public static PermissionTarget client(ClientDatabaseId clientDatabaseId) {
        return new PermissionTarget(PermissionGroupType.GLOBAL_CLIENT, clientDatabaseId.value(), 0);
    }

    public static PermissionTarget channelClient(ChannelId channelId, ClientDatabaseId clientDatabaseId) {
        return new PermissionTarget(PermissionGroupType.CHANNEL_CLIENT, channelId.value(), clientDatabaseId.value());
    }
}
