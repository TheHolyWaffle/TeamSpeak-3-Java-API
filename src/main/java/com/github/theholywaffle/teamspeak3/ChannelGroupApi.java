package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroupClient;

import java.util.List;

/**
 * Channel group administration. Channel group permissions are managed through
 * {@link PermissionApi} with {@link com.github.theholywaffle.teamspeak3.api.PermissionTarget#channelGroup(int)}.
 *
 * @see ChannelGroupApiAsync the asynchronous form
 */
public final class ChannelGroupApi {

	private final ChannelGroupApiAsync async;

	ChannelGroupApi(ChannelGroupApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists all channel groups.
	 *
	 * @return the channel groups
	 */
	public List<ChannelGroup> list() {
		return CommandFutures.join(async.list());
	}

	/**
	 * Creates a regular channel group.
	 *
	 * @param name the group name
	 * @return the ID of the new group
	 */
	public int create(String name) {
		return CommandFutures.join(async.create(name));
	}

	/**
	 * Creates a channel group of the given type.
	 *
	 * @param name the group name
	 * @param type the group type, or {@code null} for a regular group
	 * @return the ID of the new group
	 */
	public int create(String name, PermissionGroupDatabaseType type) {
		return CommandFutures.join(async.create(name, type));
	}

	/**
	 * Copies a channel group into a new group.
	 *
	 * @param sourceGroupId the group to copy
	 * @param name the new group's name
	 * @param type the new group's type
	 * @return the ID of the new group
	 */
	public int copy(int sourceGroupId, String name, PermissionGroupDatabaseType type) {
		return CommandFutures.join(async.copy(sourceGroupId, name, type));
	}

	/**
	 * Overwrites an existing channel group with a copy of another group's permissions.
	 *
	 * @param sourceGroupId the group to copy
	 * @param targetGroupId the existing group to overwrite
	 * @param type the target group's type
	 */
	public void overwrite(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
		CommandFutures.join(async.overwrite(sourceGroupId, targetGroupId, type));
	}

	/**
	 * Renames a channel group.
	 *
	 * @param groupId the group ID
	 * @param name the new name
	 */
	public void rename(int groupId, String name) {
		CommandFutures.join(async.rename(groupId, name));
	}

	/**
	 * Deletes a channel group, even if it still has members.
	 *
	 * @param groupId the group ID
	 */
	public void delete(int groupId) {
		CommandFutures.join(async.delete(groupId));
	}

	/**
	 * Deletes a channel group.
	 *
	 * @param groupId the group ID
	 * @param force whether to delete a group that still has members
	 */
	public void delete(int groupId, boolean force) {
		CommandFutures.join(async.delete(groupId, force));
	}

	/**
	 * Lists channel group memberships.
	 *
	 * @param filter the channel, client and group criteria; {@link ChannelGroupFilter#all()} lists every membership
	 * @return the matching memberships; empty if none match
	 */
	public List<ChannelGroupClient> members(ChannelGroupFilter filter) {
		return CommandFutures.join(async.members(filter));
	}

	/**
	 * Sets a database client's channel group in one channel.
	 *
	 * @param groupId the channel group ID
	 * @param channelId the channel ID
	 * @param clientDatabaseId the client database ID
	 */
	public void assign(int groupId, int channelId, ClientDatabaseId clientDatabaseId) {
		CommandFutures.join(async.assign(groupId, channelId, clientDatabaseId));
	}
}
