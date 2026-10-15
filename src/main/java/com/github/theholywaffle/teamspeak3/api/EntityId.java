package com.github.theholywaffle.teamspeak3.api;

/** A typed ID whose {@link #value()} is the number sent in ServerQuery commands. */
public sealed interface EntityId
        permits ApiKeyId,
                BanId,
                ChannelGroupId,
                ChannelId,
                ClientDatabaseId,
                IconId,
                MessageId,
                ServerGroupId,
                VirtualServerId {

    /** @return the positive ID */
    long value();
}
