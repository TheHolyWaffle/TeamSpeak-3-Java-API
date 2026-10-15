package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Executes the commands of the shared domain APIs: a ServerQuery session or Web Query requests. */
interface QueryBackend {

    /**
     * Admits one command, completing its future with the response or a failure.
     *
     * @param work settles after asynchronous library work that depends on the response
     */
    void enqueue(Command command, CompletionStage<?> work);

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException without a notification stream */
    EventSubscription subscribe(TS3Listener listener);

    /**
     * Addresses a virtual server for the commands sent through the returned backend. ServerQuery
     * selects it with {@code use}, so it stays selected for the session; Web Query only changes the
     * request URL.
     */
    CompletableFuture<QueryBackend> onVirtualServer(VirtualServerId serverId);

    /** Runs a file transfer, or fails it when the backend has no file-transfer support. */
    <T> CompletableFuture<T> withFileTransfers(Function<FileTransfers, CompletableFuture<T>> transfer);
}
