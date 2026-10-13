package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous server notification registration and local listener subscription.
 *
 * @see EventApi the synchronous form and full documentation
 */
public final class EventApiAsync {

    private final QueryRequests requests;

    EventApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see EventApi#register(TS3EventType...) */
    public CompletableFuture<Void> register(TS3EventType... eventTypes) {
        var registrations = Arrays.stream(eventTypes)
                .map(type -> register(type, defaultChannel(type)))
                .toList();
        return QueryRequests.discard(CommandFutures.all(registrations));
    }

    /** @see EventApi#register(TS3EventType, int) */
    public CompletableFuture<Void> register(TS3EventType eventType, int channelId) {
        return requests.run(QueryCommands.serverNotifyRegister(eventType, channelId));
    }

    /** @see EventApi#registerAll() */
    public CompletableFuture<Void> registerAll() {
        return register(TS3EventType.values());
    }

    /** @see EventApi#unregisterAll() */
    public CompletableFuture<Void> unregisterAll() {
        return requests.run(QueryCommands.serverNotifyUnregister());
    }

    /** @see EventApi#subscribe(TS3Listener) */
    public EventSubscription subscribe(TS3Listener listener) {
        return requests.query().subscribe(listener);
    }

    private static int defaultChannel(TS3EventType type) {
        return type == TS3EventType.CHANNEL || type == TS3EventType.TEXT_CHANNEL ? 0 : -1;
    }
}
