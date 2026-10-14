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
    public EventSubscription subscribe(TS3Listener listener) {
        throw new TS3UnsupportedOperationException(
                "Web Query has no notification stream; use a RAW or SSH TS3Query for events");
    }

    @Override
    public <T> CompletableFuture<T> withFileTransfers(Function<FileTransfers, CompletableFuture<T>> transfer) {
        return CompletableFuture.failedFuture(new TS3UnsupportedOperationException(
                "Web Query does not support file transfers; use a RAW or SSH TS3Query"));
    }
}
