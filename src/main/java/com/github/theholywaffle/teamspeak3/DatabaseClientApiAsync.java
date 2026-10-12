package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.CustomPropertyAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.CustomPropertyCommands;
import com.github.theholywaffle.teamspeak3.commands.DatabaseClientCommands;

import java.util.ArrayList;
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

	/** @see DatabaseClientApi#info(int) */
	public CompletableFuture<DatabaseClientInfo> info(int databaseId) {
		return requests.model(DatabaseClientCommands.clientDBInfo(databaseId), DatabaseClientInfo::new);
	}

	/** @see DatabaseClientApi#search(String) */
	public CompletableFuture<List<DatabaseClientInfo>> search(String nicknamePattern) {
		var ids = requests.integers(DatabaseClientCommands.clientDBFind(nicknamePattern, false), "cldbid");
		return CommandFutures.compose(ids, found -> CommandFutures.all(found.stream().map(this::info).toList()));
	}

	/** @see DatabaseClientApi#findByUniqueId(String) */
	public CompletableFuture<Optional<DatabaseClientInfo>> findByUniqueId(String uniqueId) {
		var id = requests.optionalInteger(DatabaseClientCommands.clientDBFind(uniqueId, true), "cldbid");
		return CommandFutures.compose(id, found -> found.isEmpty()
			? CompletableFuture.completedFuture(Optional.empty())
			: CommandFutures.map(info(found.get()), Optional::of));
	}

	/** @see DatabaseClientApi#edit(int, Map) */
	public CompletableFuture<Void> edit(int databaseId, Map<ClientProperty, String> properties) {
		return requests.run(DatabaseClientCommands.clientDBEdit(databaseId, properties));
	}

	/** @see DatabaseClientApi#delete(int) */
	public CompletableFuture<Void> delete(int databaseId) {
		return requests.run(DatabaseClientCommands.clientDBDelete(databaseId));
	}

	/** @see DatabaseClientApi#customProperties(int) */
	public CompletableFuture<Map<String, String>> customProperties(int databaseId) {
		return requests.response(CustomPropertyCommands.customInfo(databaseId), response -> {
			Map<String, String> properties = new LinkedHashMap<>();
			for (Wrapper row : response.getResponses()) properties.put(row.get("ident"), row.get("value"));
			return java.util.Collections.unmodifiableMap(properties);
		});
	}

	/** @see DatabaseClientApi#setCustomProperty(int, String, String) */
	public CompletableFuture<Void> setCustomProperty(int databaseId, String key, String value) {
		Objects.requireNonNull(key, "key");
		return requests.run(CustomPropertyCommands.customSet(databaseId, key, value));
	}

	/** @see DatabaseClientApi#setCustomProperties(int, Map) */
	public CompletableFuture<Void> setCustomProperties(int databaseId, Map<String, String> properties) {
		var updates = properties.entrySet().stream()
			.map(entry -> setCustomProperty(databaseId, entry.getKey(), entry.getValue())).toList();
		return QueryRequests.discard(CommandFutures.all(updates));
	}

	/** @see DatabaseClientApi#deleteCustomProperty(int, String) */
	public CompletableFuture<Void> deleteCustomProperty(int databaseId, String key) {
		Objects.requireNonNull(key, "key");
		return requests.run(CustomPropertyCommands.customDelete(databaseId, key));
	}

	/** @see DatabaseClientApi#searchCustomProperty(String, String) */
	public CompletableFuture<List<CustomPropertyAssignment>> searchCustomProperty(String key, String valuePattern) {
		Objects.requireNonNull(key, "key");
		return requests.models(CustomPropertyCommands.customSearch(key, valuePattern), CustomPropertyAssignment::new);
	}
}
