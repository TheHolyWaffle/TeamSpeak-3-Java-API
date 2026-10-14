package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Complaint;
import com.github.theholywaffle.teamspeak3.commands.ComplaintCommands;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous complaint administration.
 *
 * @see ComplaintApi the synchronous form and full documentation
 */
public final class ComplaintApiAsync {

    private final QueryRequests requests;

    ComplaintApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see ComplaintApi#list() */
    public CompletableFuture<List<Complaint>> list() {
        return requests.models(ComplaintCommands.complainList(-1), Complaint::new);
    }

    /** @see ComplaintApi#about(ClientDatabaseId) */
    public CompletableFuture<List<Complaint>> about(ClientDatabaseId targetDatabaseId) {
        return requests.models(ComplaintCommands.complainList(targetDatabaseId.value()), Complaint::new);
    }

    /** @see ComplaintApi#add(ClientDatabaseId, String) */
    public CompletableFuture<Void> add(ClientDatabaseId targetDatabaseId, String message) {
        return requests.run(ComplaintCommands.complainAdd(targetDatabaseId.value(), message));
    }

    /** @see ComplaintApi#delete(ClientDatabaseId, ClientDatabaseId) */
    public CompletableFuture<Void> delete(ClientDatabaseId targetDatabaseId, ClientDatabaseId fromDatabaseId) {
        return requests.run(ComplaintCommands.complainDel(targetDatabaseId.value(), fromDatabaseId.value()));
    }

    /** @see ComplaintApi#deleteAll(ClientDatabaseId) */
    public CompletableFuture<Void> deleteAll(ClientDatabaseId targetDatabaseId) {
        return requests.run(ComplaintCommands.complainDelAll(targetDatabaseId.value()));
    }
}
