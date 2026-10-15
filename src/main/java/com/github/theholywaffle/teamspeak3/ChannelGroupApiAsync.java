package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
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
    public CompletableFuture<ChannelGroupId> create(String name) {
        return create(name, null);
    }

    /** @see ChannelGroupApi#create(String, PermissionGroupDatabaseType) */
    public CompletableFuture<ChannelGroupId> create(String name, PermissionGroupDatabaseType type) {
        return requests.id(ChannelGroupCommands.channelGroupAdd(name, type), "cgid", ChannelGroupId::of);
    }

    /** @see ChannelGroupApi#copy(ChannelGroupId, String, PermissionGroupDatabaseType) */
    public CompletableFuture<ChannelGroupId> copy(
            ChannelGroupId sourceGroupId, String name, PermissionGroupDatabaseType type) {
        return requests.id(
                ChannelGroupCommands.channelGroupCopy(sourceGroupId, name, type), "cgid", ChannelGroupId::of);
    }

    /** @see ChannelGroupApi#overwrite(ChannelGroupId, ChannelGroupId, PermissionGroupDatabaseType) */
    public CompletableFuture<Void> overwrite(
            ChannelGroupId sourceGroupId, ChannelGroupId targetGroupId, PermissionGroupDatabaseType type) {
        return requests.run(ChannelGroupCommands.channelGroupCopy(sourceGroupId, targetGroupId, type));
    }

    /** @see ChannelGroupApi#rename(ChannelGroupId, String) */
    public CompletableFuture<Void> rename(ChannelGroupId groupId, String name) {
        return requests.run(ChannelGroupCommands.channelGroupRename(groupId, name));
    }

    /** @see ChannelGroupApi#delete(ChannelGroupId) */
    public CompletableFuture<Void> delete(ChannelGroupId groupId) {
        return delete(groupId, true);
    }

    /** @see ChannelGroupApi#delete(ChannelGroupId, boolean) */
    public CompletableFuture<Void> delete(ChannelGroupId groupId, boolean force) {
        return requests.run(ChannelGroupCommands.channelGroupDel(groupId, force));
    }

    /** @see ChannelGroupApi#members(ChannelGroupFilter) */
    public CompletableFuture<List<ChannelGroupClient>> members(ChannelGroupFilter filter) {
        return requests.rows(ChannelGroupCommands.channelGroupClientList(filter), ChannelGroupClient::from);
    }

    /** @see ChannelGroupApi#assign(ChannelGroupId, ChannelId, ClientDatabaseId) */
    public CompletableFuture<Void> assign(
            ChannelGroupId groupId, ChannelId channelId, ClientDatabaseId clientDatabaseId) {
        return requests.run(ChannelGroupCommands.setClientChannelGroup(groupId, channelId, clientDatabaseId));
    }
}
