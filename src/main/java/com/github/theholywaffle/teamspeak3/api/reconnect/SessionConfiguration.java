package com.github.theholywaffle.teamspeak3.api.reconnect;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable session state restored before queued commands on every connection.
 * Authentication uses TS3Config login credentials (RAW login or SSH authentication).
 * A virtual server ID is mandatory, avoiding a reconnect to an implicit server.
 * @param virtualServerId stable target virtual server ID
 * @param nickname query nickname, or null to retain the server default
 * @param subscriptions ordered notification registrations
 */
public record SessionConfiguration(int virtualServerId, String nickname, List<Subscription> subscriptions) {
    /**
     * One notification registration.
     * @param eventType event type
     * @param channelId channel ID, or -1 to omit it
     */
    public record Subscription(TS3EventType eventType, int channelId) {
        public Subscription {
            Objects.requireNonNull(eventType, "eventType");
            if (channelId < -1) throw new IllegalArgumentException("Channel ID must be >= -1");
        }
    }

    public SessionConfiguration {
        if (virtualServerId <= 0) throw new IllegalArgumentException("Virtual server ID must be positive");
        subscriptions = List.copyOf(subscriptions);
        if (nickname != null && nickname.isEmpty()) throw new IllegalArgumentException("Nickname must not be empty");
    }

    /**
     * @param id target virtual server ID
     * @return an empty session for that server
     */
    public static SessionConfiguration forServer(int id) {
        return new SessionConfiguration(id, null, List.of());
    }

    /**
     * @param name nickname
     * @return a copy with that nickname
     */
    public SessionConfiguration withNickname(String name) {
        return new SessionConfiguration(virtualServerId, name, subscriptions);
    }

    /**
     * @param type event type
     * @param channelId channel ID
     * @return a copy with the appended registration
     */
    public SessionConfiguration withSubscription(TS3EventType type, int channelId) {
        var copy = new ArrayList<>(subscriptions);
        copy.add(new Subscription(type, channelId));
        return new SessionConfiguration(virtualServerId, nickname, copy);
    }

    /**
     * Restores state synchronously; any failure prevents application command delivery.
     * @param api initialization API
     */
    public void restore(TS3Api api) {
        api.selectVirtualServerById(virtualServerId);
        if (nickname != null) api.setNickname(nickname);
        for (Subscription subscription : subscriptions)
            api.events().register(subscription.eventType(), subscription.channelId());
    }
}
