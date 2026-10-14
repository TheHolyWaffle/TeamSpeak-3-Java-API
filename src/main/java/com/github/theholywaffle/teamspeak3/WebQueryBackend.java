package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Sends each command as one Web Query request addressed to {@code scope}. */
record WebQueryBackend(TS3WebQuery query, String scope) implements QueryBackend {

    @Override
    public void enqueue(Command command, CompletionStage<?> work) {
        query.send(scope, command, work);
    }

    @Override
    public CompletableFuture<QueryBackend> onVirtualServer(int serverId) {
        if (serverId < 1) throw new IllegalArgumentException("Virtual server ID must be positive");
        return CompletableFuture.completedFuture(new WebQueryBackend(query, serverId + "/"));
    }

    @Override
    public EventSubscription subscribe(TS3Listener listener) {
        throw new TS3UnsupportedOperationException(WebQueryProtocol.NO_EVENTS);
    }

    @Override
    public <T> CompletableFuture<T> withFileTransfers(Function<FileTransfers, CompletableFuture<T>> transfer) {
        return CompletableFuture.failedFuture(new TS3UnsupportedOperationException(WebQueryProtocol.NO_FILES));
    }
}
