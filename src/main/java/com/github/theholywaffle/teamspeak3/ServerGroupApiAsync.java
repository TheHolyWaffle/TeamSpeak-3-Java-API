package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroupClient;
import com.github.theholywaffle.teamspeak3.commands.ServerGroupCommands;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous server group administration.
 *
 * @see ServerGroupApi the synchronous form and full documentation
 */
public final class ServerGroupApiAsync {

    private final QueryRequests requests;

    ServerGroupApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see ServerGroupApi#list() */
    public CompletableFuture<List<ServerGroup>> list() {
        return requests.models(ServerGroupCommands.serverGroupList(), ServerGroup::new);
    }

    /** @see ServerGroupApi#create(String) */
    public CompletableFuture<ServerGroupId> create(String name) {
        return create(name, PermissionGroupDatabaseType.REGULAR);
    }

    /** @see ServerGroupApi#create(String, PermissionGroupDatabaseType) */
    public CompletableFuture<ServerGroupId> create(String name, PermissionGroupDatabaseType type) {
        return requests.id(ServerGroupCommands.serverGroupAdd(name, type), "sgid", ServerGroupId::of);
    }

    /** @see ServerGroupApi#copy(ServerGroupId, String, PermissionGroupDatabaseType) */
    public CompletableFuture<ServerGroupId> copy(
            ServerGroupId sourceGroupId, String name, PermissionGroupDatabaseType type) {
        return requests.id(ServerGroupCommands.serverGroupCopy(sourceGroupId, name, type), "sgid", ServerGroupId::of);
    }

    /** @see ServerGroupApi#overwrite(ServerGroupId, ServerGroupId, PermissionGroupDatabaseType) */
    public CompletableFuture<Void> overwrite(
            ServerGroupId sourceGroupId, ServerGroupId targetGroupId, PermissionGroupDatabaseType type) {
        return requests.run(ServerGroupCommands.serverGroupCopy(sourceGroupId, targetGroupId, type));
    }

    /** @see ServerGroupApi#rename(ServerGroupId, String) */
    public CompletableFuture<Void> rename(ServerGroupId groupId, String name) {
        return requests.run(ServerGroupCommands.serverGroupRename(groupId, name));
    }

    /** @see ServerGroupApi#delete(ServerGroupId) */
    public CompletableFuture<Void> delete(ServerGroupId groupId) {
        return delete(groupId, true);
    }

    /** @see ServerGroupApi#delete(ServerGroupId, boolean) */
    public CompletableFuture<Void> delete(ServerGroupId groupId, boolean force) {
        return requests.run(ServerGroupCommands.serverGroupDel(groupId, force));
    }

    /** @see ServerGroupApi#members(ServerGroupId) */
    public CompletableFuture<List<ServerGroupClient>> members(ServerGroupId groupId) {
        return requests.rows(ServerGroupCommands.serverGroupClientList(groupId), ServerGroupClient::from);
    }

    /** @see ServerGroupApi#addMember(ServerGroupId, ClientDatabaseId) */
    public CompletableFuture<Void> addMember(ServerGroupId groupId, ClientDatabaseId clientDatabaseId) {
        return requests.run(ServerGroupCommands.serverGroupAddClient(groupId, clientDatabaseId));
    }

    /** @see ServerGroupApi#removeMember(ServerGroupId, ClientDatabaseId) */
    public CompletableFuture<Void> removeMember(ServerGroupId groupId, ClientDatabaseId clientDatabaseId) {
        return requests.run(ServerGroupCommands.serverGroupDelClient(groupId, clientDatabaseId));
    }

    /** @see ServerGroupApi#ofClient(ClientDatabaseId) */
    public CompletableFuture<List<ServerGroup>> ofClient(ClientDatabaseId clientDatabaseId) {
        var ids = requests.ids(ServerGroupCommands.serverGroupsByClientId(clientDatabaseId), "sgid", ServerGroupId::of);
        return QueryRequests.findByKey(ids, list(), ServerGroup::getId);
    }
}
