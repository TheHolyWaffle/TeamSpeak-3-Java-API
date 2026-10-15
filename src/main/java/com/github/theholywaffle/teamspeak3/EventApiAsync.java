package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
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
                .map(type -> requests.run(QueryCommands.serverNotifyRegister(type, defaultChannel(type))))
                .toList();
        return QueryRequests.discard(CommandFutures.all(registrations));
    }

    /** @see EventApi#register(TS3EventType, ChannelId) */
    public CompletableFuture<Void> register(TS3EventType eventType, ChannelId channelId) {
        return requests.run(QueryCommands.serverNotifyRegister(eventType, channelId.value()));
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
        return requests.backend().subscribe(listener);
    }

    private static int defaultChannel(TS3EventType type) {
        return type == TS3EventType.CHANNEL || type == TS3EventType.TEXT_CHANNEL ? 0 : -1;
    }
}
