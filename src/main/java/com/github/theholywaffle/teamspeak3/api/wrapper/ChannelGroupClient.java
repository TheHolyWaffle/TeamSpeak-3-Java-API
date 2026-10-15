package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Objects;

/**
 * A channel group membership from {@code channelgroupclientlist}.
 *
 * @param channelId the channel in which the client has the group
 * @param clientDatabaseId the member
 * @param channelGroupId the channel group
 */
public record ChannelGroupClient(
        ChannelId channelId, ClientDatabaseId clientDatabaseId, ChannelGroupId channelGroupId) {

    public ChannelGroupClient {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
        Objects.requireNonNull(channelGroupId, "channelGroupId");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static ChannelGroupClient from(Wrapper row) {
        return new ChannelGroupClient(
                row.requireId("cid", ChannelId::new),
                row.requireDatabaseId("cldbid"),
                row.requireId("cgid", ChannelGroupId::new));
    }
}
