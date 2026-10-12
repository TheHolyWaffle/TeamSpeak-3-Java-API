package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroupClient;
import com.github.theholywaffle.teamspeak3.commands.ChannelGroupCommands;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous channel group administration.
 *
 * @see ChannelGroupApi the synchronous form and full documentation
 */
public final class ChannelGroupApiAsync {

	private final QueryRequests requests;

	ChannelGroupApiAsync(QueryRequests requests) {
		this.requests = requests;
	}

	/** @see ChannelGroupApi#list() */
	public CompletableFuture<List<ChannelGroup>> list() {
		return requests.models(ChannelGroupCommands.channelGroupList(), ChannelGroup::new);
	}

	/** @see ChannelGroupApi#create(String) */
	public CompletableFuture<Integer> create(String name) {
		return create(name, null);
	}

	/** @see ChannelGroupApi#create(String, PermissionGroupDatabaseType) */
	public CompletableFuture<Integer> create(String name, PermissionGroupDatabaseType type) {
		return requests.createdId(ChannelGroupCommands.channelGroupAdd(name, type), "cgid");
	}

	/** @see ChannelGroupApi#copy(int, String, PermissionGroupDatabaseType) */
	public CompletableFuture<Integer> copy(int sourceGroupId, String name, PermissionGroupDatabaseType type) {
		return requests.createdId(ChannelGroupCommands.channelGroupCopy(sourceGroupId, name, type), "cgid");
	}

	/** @see ChannelGroupApi#overwrite(int, int, PermissionGroupDatabaseType) */
	public CompletableFuture<Void> overwrite(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		if (targetGroupId <= 0) throw new IllegalArgumentException("Target group ID must be positive; use copy to create a group");
		return requests.run(ChannelGroupCommands.channelGroupCopy(sourceGroupId, targetGroupId, type));
	}

	/** @see ChannelGroupApi#rename(int, String) */
	public CompletableFuture<Void> rename(int groupId, String name) {
		return requests.run(ChannelGroupCommands.channelGroupRename(groupId, name));
	}

	/** @see ChannelGroupApi#delete(int) */
	public CompletableFuture<Void> delete(int groupId) {
		return delete(groupId, true);
	}

	/** @see ChannelGroupApi#delete(int, boolean) */
	public CompletableFuture<Void> delete(int groupId, boolean force) {
		return requests.run(ChannelGroupCommands.channelGroupDel(groupId, force));
	}

	/** @see ChannelGroupApi#members(ChannelGroupFilter) */
	public CompletableFuture<List<ChannelGroupClient>> members(ChannelGroupFilter filter) {
		return requests.models(ChannelGroupCommands.channelGroupClientList(filter.channelId().orElse(-1),
			filter.clientDatabaseId().orElse(-1), filter.groupId().orElse(-1)), ChannelGroupClient::new);
	}

	/** @see ChannelGroupApi#assign(int, int, int) */
	public CompletableFuture<Void> assign(int groupId, int channelId, int clientDatabaseId) {
		return requests.run(ChannelGroupCommands.setClientChannelGroup(groupId, channelId, clientDatabaseId));
	}
}
