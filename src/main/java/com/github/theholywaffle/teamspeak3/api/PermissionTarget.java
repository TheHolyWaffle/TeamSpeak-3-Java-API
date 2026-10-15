package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;

/**
 * The holder of a permission assignment: a server group, channel group, channel, client, or client
 * within one channel. Create it with the factories.
 */
public sealed interface PermissionTarget {

    static ServerGroupTarget serverGroup(ServerGroupId serverGroupId) {
        return new ServerGroupTarget(serverGroupId);
    }

    static ChannelGroupTarget channelGroup(ChannelGroupId channelGroupId) {
        return new ChannelGroupTarget(channelGroupId);
    }

    static ChannelTarget channel(ChannelId channelId) {
        return new ChannelTarget(channelId);
    }

    static ClientTarget client(ClientDatabaseId clientDatabaseId) {
        return new ClientTarget(clientDatabaseId);
    }

    static ChannelClientTarget channelClient(ChannelId channelId, ClientDatabaseId clientDatabaseId) {
        return new ChannelClientTarget(channelId, clientDatabaseId);
    }

    /** @return the type of holder */
    PermissionGroupType type();

    /** @param serverGroupId the server group */
    record ServerGroupTarget(ServerGroupId serverGroupId) implements PermissionTarget {
        public ServerGroupTarget {
            Objects.requireNonNull(serverGroupId, "serverGroupId");
        }

        @Override
        public PermissionGroupType type() {
            return PermissionGroupType.SERVER_GROUP;
        }
    }

    /** @param channelGroupId the channel group */
    record ChannelGroupTarget(ChannelGroupId channelGroupId) implements PermissionTarget {
        public ChannelGroupTarget {
            Objects.requireNonNull(channelGroupId, "channelGroupId");
        }

        @Override
        public PermissionGroupType type() {
            return PermissionGroupType.CHANNEL_GROUP;
        }
    }

    /** @param channelId the channel */
    record ChannelTarget(ChannelId channelId) implements PermissionTarget {
        public ChannelTarget {
            Objects.requireNonNull(channelId, "channelId");
        }

        @Override
        public PermissionGroupType type() {
            return PermissionGroupType.CHANNEL;
        }
    }

    /** @param clientDatabaseId the client, in every channel */
    record ClientTarget(ClientDatabaseId clientDatabaseId) implements PermissionTarget {
        public ClientTarget {
            Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        }

        @Override
        public PermissionGroupType type() {
            return PermissionGroupType.GLOBAL_CLIENT;
        }
    }

    /**
     * @param channelId the channel
     * @param clientDatabaseId the client within that channel
     */
    record ChannelClientTarget(ChannelId channelId, ClientDatabaseId clientDatabaseId) implements PermissionTarget {
        public ChannelClientTarget {
            Objects.requireNonNull(channelId, "channelId");
            Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        }

        @Override
        public PermissionGroupType type() {
            return PermissionGroupType.CHANNEL_CLIENT;
        }
    }
}
