package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.util.Objects;

/**
 * A channel group membership from {@code channelgroupclientlist}.
 *
 * @param channelId the channel in which the client has the group
 * @param clientDatabaseId the member
 * @param channelGroupId the channel group
 */
public record ChannelGroupClient(int channelId, ClientDatabaseId clientDatabaseId, int channelGroupId) {

    public ChannelGroupClient {
        Objects.requireNonNull(clientDatabaseId, "clientDatabaseId");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
    public static ChannelGroupClient from(Wrapper row) {
        return new ChannelGroupClient(row.requireInt("cid"), row.requireDatabaseId("cldbid"), row.requireInt("cgid"));
    }
}
