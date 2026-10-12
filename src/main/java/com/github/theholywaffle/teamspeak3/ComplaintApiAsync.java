package com.github.theholywaffle.teamspeak3;

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

	/** @see ComplaintApi#about(int) */
	public CompletableFuture<List<Complaint>> about(int targetDatabaseId) {
		return requests.models(ComplaintCommands.complainList(targetDatabaseId), Complaint::new);
	}

	/** @see ComplaintApi#add(int, String) */
	public CompletableFuture<Void> add(int targetDatabaseId, String message) {
		return requests.run(ComplaintCommands.complainAdd(targetDatabaseId, message));
	}

	/** @see ComplaintApi#delete(int, int) */
	public CompletableFuture<Void> delete(int targetDatabaseId, int fromDatabaseId) {
		return requests.run(ComplaintCommands.complainDel(targetDatabaseId, fromDatabaseId));
	}

	/** @see ComplaintApi#deleteAll(int) */
	public CompletableFuture<Void> deleteAll(int targetDatabaseId) {
		return requests.run(ComplaintCommands.complainDelAll(targetDatabaseId));
	}
}
