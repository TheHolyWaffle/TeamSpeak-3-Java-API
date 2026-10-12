package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.wrapper.CreatedQueryLogin;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryLogin;
import com.github.theholywaffle.teamspeak3.commands.ClientCommands;
import com.github.theholywaffle.teamspeak3.commands.QueryLoginCommands;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous ServerQuery login administration.
 *
 * @see QueryLoginApi the synchronous form and full documentation
 */
public final class QueryLoginApiAsync {

	private final QueryRequests requests;

	QueryLoginApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see QueryLoginApi#list() */
	public CompletableFuture<List<QueryLogin>> list() {
		return search(null);
	}

	/** @see QueryLoginApi#search(String) */
	public CompletableFuture<List<QueryLogin>> search(String loginNamePattern) {
		return requests.models(QueryLoginCommands.queryLoginList(loginNamePattern), QueryLogin::new);
	}

	/** @see QueryLoginApi#create(String, int) */
	public CompletableFuture<CreatedQueryLogin> create(String loginName, int clientDatabaseId) {
		return requests.model(QueryLoginCommands.queryLoginAdd(loginName, clientDatabaseId), CreatedQueryLogin::new);
	}

	/** @see QueryLoginApi#delete(int) */
	public CompletableFuture<Void> delete(int clientDatabaseId) {
		return requests.run(QueryLoginCommands.queryLoginDel(clientDatabaseId));
	}

	/** @see QueryLoginApi#updateOwn(String) */
	public CompletableFuture<String> updateOwn(String loginName) {
		return requests.string(ClientCommands.clientSetServerQueryLogin(loginName), "client_login_password");
	}
}
