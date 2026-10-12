package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.CustomPropertyAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.CustomPropertyCommands;
import com.github.theholywaffle.teamspeak3.commands.DatabaseClientCommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous administration of clients known to the virtual server's database,
 * including their custom properties.
 *
 * @see DatabaseClientApi the synchronous form and full documentation
 */
public final class DatabaseClientApiAsync {

	private static final int PAGE_SIZE = 200;

	private final QueryRequests requests;

	DatabaseClientApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see DatabaseClientApi#list() */
	public CompletableFuture<List<DatabaseClient>> list() {
		var count = requests.integer(DatabaseClientCommands.clientDBList(0, 1, true), "count");
		return CommandFutures.map(CommandFutures.compose(count, total -> {
			List<CompletableFuture<List<DatabaseClient>>> pages = new ArrayList<>((total + PAGE_SIZE - 1) / PAGE_SIZE);
			for (int offset = 0; offset < total; offset += PAGE_SIZE) pages.add(list(offset, PAGE_SIZE));
			return CommandFutures.all(pages);
		}), pages -> pages.stream().flatMap(List::stream).toList());
	}

	/** @see DatabaseClientApi#list(int, int) */
	public CompletableFuture<List<DatabaseClient>> list(int offset, int count) {
		return requests.models(DatabaseClientCommands.clientDBList(offset, count, false), DatabaseClient::new);
	}

	/** @see DatabaseClientApi#info(ClientDatabaseId) */
	public CompletableFuture<DatabaseClientInfo> info(ClientDatabaseId databaseId) {
		return requests.model(DatabaseClientCommands.clientDBInfo(databaseId.value()), DatabaseClientInfo::new);
	}

	/** @see DatabaseClientApi#search(String) */
	public CompletableFuture<List<DatabaseClientInfo>> search(String nicknamePattern) {
		var ids = requests.rows(DatabaseClientCommands.clientDBFind(nicknamePattern, false), row -> row.requireDatabaseId("cldbid"));
		return CommandFutures.compose(ids, found -> CommandFutures.all(found.stream().map(this::info).toList()));
	}

	/** @see DatabaseClientApi#findByUniqueId(String) */
	public CompletableFuture<Optional<DatabaseClientInfo>> findByUniqueId(String uniqueId) {
		var id = requests.optional(DatabaseClientCommands.clientDBFind(uniqueId, true), row -> row.requireDatabaseId("cldbid"));
		return QueryRequests.thenLookup(id, this::info);
	}

	/** @see DatabaseClientApi#edit(ClientDatabaseId, Map) */
	public CompletableFuture<Void> edit(ClientDatabaseId databaseId, Map<ClientProperty, String> properties) {
		return requests.run(DatabaseClientCommands.clientDBEdit(databaseId.value(), properties));
	}

	/** @see DatabaseClientApi#delete(ClientDatabaseId) */
	public CompletableFuture<Void> delete(ClientDatabaseId databaseId) {
		return requests.run(DatabaseClientCommands.clientDBDelete(databaseId.value()));
	}

	/** @see DatabaseClientApi#customProperties(ClientDatabaseId) */
	public CompletableFuture<Map<String, String>> customProperties(ClientDatabaseId databaseId) {
		return requests.response(CustomPropertyCommands.customInfo(databaseId.value()), response -> {
			Map<String, String> properties = new LinkedHashMap<>();
			for (Wrapper row : response.getResponses()) properties.put(row.require("ident"), row.require("value"));
			return Collections.unmodifiableMap(properties);
		});
	}

	/** @see DatabaseClientApi#setCustomProperty(ClientDatabaseId, String, String) */
	public CompletableFuture<Void> setCustomProperty(ClientDatabaseId databaseId, String key, String value) {
		Objects.requireNonNull(key, "key");
		return requests.run(CustomPropertyCommands.customSet(databaseId.value(), key, value));
	}

	/** @see DatabaseClientApi#setCustomProperties(ClientDatabaseId, Map) */
	public CompletableFuture<Void> setCustomProperties(ClientDatabaseId databaseId, Map<String, String> properties) {
		var updates = properties.entrySet().stream()
			.map(entry -> setCustomProperty(databaseId, entry.getKey(), entry.getValue())).toList();
		return QueryRequests.discard(CommandFutures.all(updates));
	}

	/** @see DatabaseClientApi#deleteCustomProperty(ClientDatabaseId, String) */
	public CompletableFuture<Void> deleteCustomProperty(ClientDatabaseId databaseId, String key) {
		Objects.requireNonNull(key, "key");
		return requests.run(CustomPropertyCommands.customDelete(databaseId.value(), key));
	}

	/** @see DatabaseClientApi#searchCustomProperty(String, String) */
	public CompletableFuture<List<CustomPropertyAssignment>> searchCustomProperty(String key, String valuePattern) {
		Objects.requireNonNull(key, "key");
		return requests.rows(CustomPropertyCommands.customSearch(key, valuePattern), CustomPropertyAssignment::from);
	}
}
