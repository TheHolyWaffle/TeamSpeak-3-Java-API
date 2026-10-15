package com.github.theholywaffle.teamspeak3.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Restricts a channel group membership listing. Empty criteria match everything.
 *
 * @param channelId the channel to list memberships in
 * @param clientDatabaseId the client whose memberships to list
 * @param groupId the channel group whose members to list
 */
public record ChannelGroupFilter(
        Optional<ChannelId> channelId, Optional<ClientDatabaseId> clientDatabaseId, Optional<ChannelGroupId> groupId) {

    private static final ChannelGroupFilter ALL =
            new ChannelGroupFilter(Optional.empty(), Optional.empty(), Optional.empty());

    public ChannelGroupFilter {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        Objects.requireNonNull(groupId, "groupId");
    }

    /** @return a filter matching every membership */
    public static ChannelGroupFilter all() {
        return ALL;
    }

    public ChannelGroupFilter inChannel(ChannelId id) {
        return new ChannelGroupFilter(Optional.of(id), clientDatabaseId, groupId);
    }

    public ChannelGroupFilter ofClient(ClientDatabaseId databaseId) {
        return new ChannelGroupFilter(channelId, Optional.of(databaseId), groupId);
    }

    public ChannelGroupFilter inGroup(ChannelGroupId id) {
        return new ChannelGroupFilter(channelId, clientDatabaseId, Optional.of(id));
    }
}
