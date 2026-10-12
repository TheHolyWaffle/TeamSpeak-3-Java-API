package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.PrivilegeKey;

import java.util.List;

/**
 * Privilege key administration.
 *
 * @see PrivilegeKeyApiAsync the asynchronous form
 */
public final class PrivilegeKeyApi {

	private final PrivilegeKeyApiAsync async;

	PrivilegeKeyApi(PrivilegeKeyApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists unused privilege keys.
	 *
	 * @return the privilege keys
	 */
	public List<PrivilegeKey> list() {
		return CommandFutures.join(async.list());
	}

	/**
	 * Creates a privilege key that grants a server group.
	 *
	 * @param serverGroupId the server group ID
	 * @param description a description, or {@code null}
	 * @return the token
	 */
	public String createForServerGroup(int serverGroupId, String description) {
		return CommandFutures.join(async.createForServerGroup(serverGroupId, description));
	}

	/**
	 * Creates a privilege key that grants a channel group in one channel.
	 *
	 * @param channelGroupId the channel group ID
	 * @param channelId the channel ID
	 * @param description a description, or {@code null}
	 * @return the token
	 */
	public String createForChannelGroup(int channelGroupId, int channelId, String description) {
		return CommandFutures.join(async.createForChannelGroup(channelGroupId, channelId, description));
	}

	/**
	 * Deletes a privilege key.
	 *
	 * @param token the token
	 */
	public void delete(String token) {
		CommandFutures.join(async.delete(token));
	}

	/**
	 * Uses a privilege key for this client. ServerQuery clients are rejected with error 516.
	 *
	 * @param token the token
	 */
	public void use(String token) {
		CommandFutures.join(async.use(token));
	}
}
