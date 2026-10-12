package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ClientBan;
import com.github.theholywaffle.teamspeak3.api.wrapper.Ban;
import com.github.theholywaffle.teamspeak3.commands.BanCommands;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous ban rule administration.
 *
 * @see BanApi the synchronous form and full documentation
 */
public final class BanApiAsync {

	private final QueryRequests requests;

	BanApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see BanApi#list() */
	public CompletableFuture<List<Ban>> list() {
		return requests.models(BanCommands.banList(), Ban::new);
	}

	/** @see BanApi#add(BanRequest) */
	public CompletableFuture<Integer> add(BanRequest ban) {
		return requests.integer(BanCommands.banAdd(ban.ip(), ban.name(), ban.uniqueId(), ban.myTeamSpeakId(),
			ban.duration().toSeconds(), ban.reason()), "banid");
	}

	/** @see BanApi#banClients(ClientBan) */
	public CompletableFuture<List<Integer>> banClients(ClientBan ban) {
		int[] ids = ban.clientIds().stream().mapToInt(Integer::intValue).toArray();
		return requests.integers(BanCommands.banClient(ids, ban.duration().toSeconds(), ban.reason(), ban.continueOnError()), "banid");
	}

	/** @see BanApi#delete(int) */
	public CompletableFuture<Void> delete(int banId) {
		return requests.run(BanCommands.banDel(banId));
	}

	/** @see BanApi#deleteAll() */
	public CompletableFuture<Void> deleteAll() {
		return requests.run(BanCommands.banDelAll());
	}
}
