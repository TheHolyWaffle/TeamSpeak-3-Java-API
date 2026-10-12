package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.commands.ClientCommands;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous administration of clients connected to the selected virtual server.
 *
 * @see ClientApi the synchronous form and full documentation
 */
public final class ClientApiAsync {

	private final QueryRequests requests;

	ClientApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see ClientApi#list() */
	public CompletableFuture<List<Client>> list() {
		return requests.models(ClientCommands.clientList(), Client::new);
	}

	/** @see ClientApi#info(ClientId) */
	public CompletableFuture<ClientInfo> info(ClientId clientId) {
		return requests.model(ClientCommands.clientInfo(clientId.value()), map -> new ClientInfo(clientId, map));
	}

	/** @see ClientApi#findByName(String, boolean) */
	public CompletableFuture<Optional<Client>> findByName(String nickname, boolean ignoreCase) {
		Objects.requireNonNull(nickname, "nickname");
		return CommandFutures.map(list(), clients -> QueryRequests.firstNamed(clients, Client::getNickname, nickname, ignoreCase));
	}

	/** @see ClientApi#search(String) */
	public CompletableFuture<List<Client>> search(String nicknamePart) {
		var ids = requests.rows(ClientCommands.clientFind(nicknamePart), row -> row.requireClientId("clid"));
		return QueryRequests.recover(QueryRequests.findByKey(ids, list(), Client::getId), QueryRequests.INVALID_CLIENT_ID, List.of());
	}

	/** @see ClientApi#findByUniqueId(String) */
	public CompletableFuture<Optional<ClientInfo>> findByUniqueId(String uniqueId) {
		var id = requests.optional(ClientCommands.clientGetIds(uniqueId), row -> row.requireClientId("clid"));
		return QueryRequests.recover(QueryRequests.thenLookup(id, this::info), QueryRequests.INVALID_CLIENT_ID, Optional.empty());
	}

	/** @see ClientApi#isOnline(ClientId) */
	public CompletableFuture<Boolean> isOnline(ClientId clientId) {
		return QueryRequests.recover(requests.response(ClientCommands.clientInfo(clientId.value()), ignored -> true), QueryRequests.INVALID_CLIENT_ID, false);
	}

	/** @see ClientApi#isOnline(String) */
	public CompletableFuture<Boolean> isOnline(String uniqueId) {
		return requests.response(ClientCommands.clientGetIds(uniqueId), response -> !response.getResponses().isEmpty());
	}

	/** @see ClientApi#move(ClientId, int) */
	public CompletableFuture<Void> move(ClientId clientId, int channelId) {
		return move(clientId, channelId, null);
	}

	/** @see ClientApi#move(ClientId, int, String) */
	public CompletableFuture<Void> move(ClientId clientId, int channelId, String channelPassword) {
		return requests.run(ClientCommands.clientMove(clientId.value(), channelId, channelPassword));
	}

	/** @see ClientApi#moveAll(Collection, int, String) */
	public CompletableFuture<Void> moveAll(Collection<ClientId> clientIds, int channelId, String channelPassword) {
		int[] ids = QueryRequests.values(clientIds);
		if (ids.length == 0) return CompletableFuture.completedFuture(null);
		return requests.run(ClientCommands.clientMove(ids, channelId, channelPassword));
	}

	/** @see ClientApi#kickFromChannel(String, ClientId...) */
	public CompletableFuture<Void> kickFromChannel(String reason, ClientId... clientIds) {
		return requests.run(ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_CHANNEL, reason, QueryRequests.values(List.of(clientIds))));
	}

	/** @see ClientApi#kickFromServer(String, ClientId...) */
	public CompletableFuture<Void> kickFromServer(String reason, ClientId... clientIds) {
		return requests.run(ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, reason, QueryRequests.values(List.of(clientIds))));
	}

	/** @see ClientApi#poke(ClientId, String) */
	public CompletableFuture<Void> poke(ClientId clientId, String message) {
		return requests.run(ClientCommands.clientPoke(clientId.value(), message));
	}

	/** @see ClientApi#edit(ClientId, Map) */
	public CompletableFuture<Void> edit(ClientId clientId, Map<ClientProperty, String> properties) {
		return requests.run(ClientCommands.clientEdit(clientId.value(), properties));
	}
}
