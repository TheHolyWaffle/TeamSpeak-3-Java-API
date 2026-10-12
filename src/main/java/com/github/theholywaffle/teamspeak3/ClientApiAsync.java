package com.github.theholywaffle.teamspeak3;

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

	/** @see ClientApi#info(int) */
	public CompletableFuture<ClientInfo> info(int clientId) {
		return requests.model(ClientCommands.clientInfo(clientId), map -> new ClientInfo(clientId, map));
	}

	/** @see ClientApi#findByName(String, boolean) */
	public CompletableFuture<Optional<Client>> findByName(String nickname, boolean ignoreCase) {
		Objects.requireNonNull(nickname, "nickname");
		return CommandFutures.map(list(), clients -> clients.stream()
			.filter(client -> ChannelApiAsync.sameName(client.getNickname(), nickname, ignoreCase)).findFirst());
	}

	/** @see ClientApi#search(String) */
	public CompletableFuture<List<Client>> search(String nicknamePart) {
		var ids = requests.integers(ClientCommands.clientFind(nicknamePart), "clid");
		return QueryRequests.recover(QueryRequests.findByKey(ids, list(), Client::getId), 512, List.of());
	}

	/** @see ClientApi#findByUniqueId(String) */
	public CompletableFuture<Optional<ClientInfo>> findByUniqueId(String uniqueId) {
		var id = requests.optional(ClientCommands.clientGetIds(uniqueId), row -> Integer.parseInt(row.get("clid")));
		return CommandFutures.compose(id, found -> found.isEmpty()
			? CompletableFuture.completedFuture(Optional.empty())
			: QueryRequests.recover(CommandFutures.map(info(found.get()), Optional::of), 512, Optional.empty()));
	}

	/** @see ClientApi#isOnline(int) */
	public CompletableFuture<Boolean> isOnline(int clientId) {
		return QueryRequests.recover(requests.response(ClientCommands.clientInfo(clientId), ignored -> true), 512, false);
	}

	/** @see ClientApi#isOnline(String) */
	public CompletableFuture<Boolean> isOnline(String uniqueId) {
		return requests.response(ClientCommands.clientGetIds(uniqueId), response -> !response.getResponses().isEmpty());
	}

	/** @see ClientApi#move(int, int) */
	public CompletableFuture<Void> move(int clientId, int channelId) {
		return move(clientId, channelId, null);
	}

	/** @see ClientApi#move(int, int, String) */
	public CompletableFuture<Void> move(int clientId, int channelId, String channelPassword) {
		return requests.run(ClientCommands.clientMove(clientId, channelId, channelPassword));
	}

	/** @see ClientApi#moveAll(Collection, int, String) */
	public CompletableFuture<Void> moveAll(Collection<Integer> clientIds, int channelId, String channelPassword) {
		int[] ids = clientIds.stream().mapToInt(Integer::intValue).toArray();
		if (ids.length == 0) return CompletableFuture.completedFuture(null);
		return requests.run(ClientCommands.clientMove(ids, channelId, channelPassword));
	}

	/** @see ClientApi#kickFromChannel(String, int...) */
	public CompletableFuture<Void> kickFromChannel(String reason, int... clientIds) {
		return requests.run(ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_CHANNEL, reason, clientIds));
	}

	/** @see ClientApi#kickFromServer(String, int...) */
	public CompletableFuture<Void> kickFromServer(String reason, int... clientIds) {
		return requests.run(ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, reason, clientIds));
	}

	/** @see ClientApi#poke(int, String) */
	public CompletableFuture<Void> poke(int clientId, String message) {
		return requests.run(ClientCommands.clientPoke(clientId, message));
	}

	/** @see ClientApi#edit(int, Map) */
	public CompletableFuture<Void> edit(int clientId, Map<ClientProperty, String> properties) {
		return requests.run(ClientCommands.clientEdit(clientId, properties));
	}
}
