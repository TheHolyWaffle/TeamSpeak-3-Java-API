package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Sends commands through one queue of a RAW or SSH {@link TS3Query}. */
record ServerQueryBackend(TS3Query query, CommandQueue queue) implements QueryBackend {

    @Override
    public void enqueue(Command command, CompletionStage<?> work) {
        queue.enqueueCommand(command, work);
    }

    @Override
    public EventSubscription subscribe(TS3Listener listener) {
        return query.subscribe(listener);
    }

    @Override
    public <T> CompletableFuture<T> withFileTransfers(Function<FileTransfers, CompletableFuture<T>> transfer) {
        return transfer.apply(new FileTransfers(
                query.getFileTransferHelper(), query.commandAdmission(queue.isGlobal()), query.callbackExecutor()));
    }
}
