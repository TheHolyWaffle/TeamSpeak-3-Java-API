package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ClientBan;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Ban;

import java.util.List;

/**
 * Ban rule administration.
 *
 * @see BanApiAsync the asynchronous form
 */
public final class BanApi {

	private final BanApiAsync async;

	BanApi(BanApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists all ban rules.
	 *
	 * @return the ban rules
	 */
	public List<Ban> list() {
		return CommandFutures.join(async.list());
	}

	/**
	 * Adds a ban rule.
	 *
	 * @param ban the patterns, duration and reason
	 * @return the ID of the new ban rule
	 */
	public int add(BanRequest ban) {
		return CommandFutures.join(async.add(ban));
	}

	/**
	 * Bans connected clients by IP, unique identifier and nickname and disconnects them.
	 *
	 * @param ban the clients, duration and reason
	 * @return the IDs of the ban rules created, in server order
	 */
	public List<Integer> banClients(ClientBan ban) {
		return CommandFutures.join(async.banClients(ban));
	}

	/**
	 * Deletes a ban rule.
	 *
	 * @param banId the ban rule ID
	 */
	public void delete(int banId) {
		CommandFutures.join(async.delete(banId));
	}

	/**
	 * Deletes all ban rules.
	 */
	public void deleteAll() {
		CommandFutures.join(async.deleteAll());
	}
}
