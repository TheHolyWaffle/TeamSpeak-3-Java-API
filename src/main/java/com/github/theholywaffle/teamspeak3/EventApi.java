package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;

/**
 * Server notification registration and local listener subscription.
 * <p>
 * Registration asks the server to send notifications for this query session; a local
 * {@link EventSubscription} receives them. Both are needed to observe events.
 * </p>
 *
 * @see EventApiAsync the asynchronous form
 */
public final class EventApi {

    private final EventApiAsync async;

    EventApi(EventApiAsync async) {
        this.async = async;
    }

    /**
     * Registers this query session for server notifications. Channel event types use every channel.
     *
     * @param eventTypes the event types
     */
    public void register(TS3EventType... eventTypes) {
        CommandFutures.join(async.register(eventTypes));
    }

    /**
     * Registers this query session for one notification type.
     *
     * @param eventType the event type
     * @param channelId the channel to observe for channel events, 0 for every channel, or -1 for other types
     */
    public void register(TS3EventType eventType, int channelId) {
        CommandFutures.join(async.register(eventType, channelId));
    }

    /**
     * Registers this query session for server, channel, text and privilege key notifications.
     */
    public void registerAll() {
        CommandFutures.join(async.registerAll());
    }

    /**
     * Unregisters this query session from all server notifications.
     */
    public void unregisterAll() {
        CommandFutures.join(async.unregisterAll());
    }

    /**
     * Registers a local listener. Equivalent to {@link TS3Query#subscribe(TS3Listener)}.
     *
     * @param listener the listener receiving ordered callbacks on the owned event executor
     * @return a handle that removes the listener when closed
     */
    public EventSubscription subscribe(TS3Listener listener) {
        return async.subscribe(listener);
    }
}
