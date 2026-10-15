package com.github.theholywaffle.teamspeak3.api.reconnect;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable session state restored before queued commands on every connection.
 * Authentication uses TS3Config login credentials (RAW login or SSH authentication).
 * A virtual server ID is mandatory, avoiding a reconnect to an implicit server.
 * @param virtualServerId stable target virtual server ID
 * @param nickname query nickname, or null to retain the server default
 * @param subscriptions ordered notification registrations
 */
public record SessionConfiguration(VirtualServerId virtualServerId, String nickname, List<Subscription> subscriptions) {
    /**
     * One notification registration.
     * @param eventType event type
     * @param channelId the one channel to observe, or empty for the event type's default
     */
    public record Subscription(TS3EventType eventType, Optional<ChannelId> channelId) {
        public Subscription {
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(channelId, "channelId");
        }
    }

    public SessionConfiguration {
        Objects.requireNonNull(virtualServerId, "virtualServerId");
        subscriptions = List.copyOf(subscriptions);
        if (nickname != null && nickname.isEmpty()) throw new IllegalArgumentException("Nickname must not be empty");
    }

    /**
     * @param id target virtual server ID
     * @return an empty session for that server
     */
    public static SessionConfiguration forServer(VirtualServerId id) {
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
     * @param type event type; channel event types observe every channel
     * @return a copy with the appended registration
     */
    public SessionConfiguration withSubscription(TS3EventType type) {
        return withSubscription(new Subscription(type, Optional.empty()));
    }

    /**
     * @param type event type
     * @param channelId the one channel to observe
     * @return a copy with the appended registration
     */
    public SessionConfiguration withSubscription(TS3EventType type, ChannelId channelId) {
        return withSubscription(new Subscription(type, Optional.of(channelId)));
    }

    private SessionConfiguration withSubscription(Subscription subscription) {
        var copy = new ArrayList<>(subscriptions);
        copy.add(subscription);
        return new SessionConfiguration(virtualServerId, nickname, copy);
    }

    /**
     * Restores state synchronously; any failure prevents application command delivery.
     * @param api initialization API
     */
    public void restore(TS3Api api) {
        api.selectVirtualServerById(virtualServerId);
        if (nickname != null) api.setNickname(nickname);
        for (Subscription subscription : subscriptions) {
            subscription
                    .channelId()
                    .ifPresentOrElse(
                            channel -> api.events().register(subscription.eventType(), channel),
                            () -> api.events().register(subscription.eventType()));
        }
    }
}
