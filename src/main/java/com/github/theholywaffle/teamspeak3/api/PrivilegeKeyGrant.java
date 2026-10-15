package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;

/** What a privilege key grants when used: a server group, or a channel group in one channel. */
public sealed interface PrivilegeKeyGrant {

    static ServerGroupGrant serverGroup(ServerGroupId serverGroupId) {
        return new ServerGroupGrant(serverGroupId);
    }

    static ChannelGroupGrant channelGroup(ChannelGroupId channelGroupId, ChannelId channelId) {
        return new ChannelGroupGrant(channelGroupId, channelId);
    }

    /** @return the key type */
    PrivilegeKeyType type();

    /** @param serverGroupId the server group */
    record ServerGroupGrant(ServerGroupId serverGroupId) implements PrivilegeKeyGrant {
        public ServerGroupGrant {
            Objects.requireNonNull(serverGroupId, "serverGroupId");
        }

        @Override
        public PrivilegeKeyType type() {
            return PrivilegeKeyType.SERVER_GROUP;
        }
    }

    /**
     * @param channelGroupId the channel group
     * @param channelId the channel the group applies in
     */
    record ChannelGroupGrant(ChannelGroupId channelGroupId, ChannelId channelId) implements PrivilegeKeyGrant {
        public ChannelGroupGrant {
            Objects.requireNonNull(channelGroupId, "channelGroupId");
            Objects.requireNonNull(channelId, "channelId");
        }

        @Override
        public PrivilegeKeyType type() {
            return PrivilegeKeyType.CHANNEL_GROUP;
        }
    }
}
