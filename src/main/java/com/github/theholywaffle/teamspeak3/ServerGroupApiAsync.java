package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
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
    public CompletableFuture<Integer> create(String name) {
        return create(name, PermissionGroupDatabaseType.REGULAR);
    }

    /** @see ServerGroupApi#create(String, PermissionGroupDatabaseType) */
    public CompletableFuture<Integer> create(String name, PermissionGroupDatabaseType type) {
        return requests.createdId(ServerGroupCommands.serverGroupAdd(name, type), "sgid");
    }

    /** @see ServerGroupApi#copy(int, String, PermissionGroupDatabaseType) */
    public CompletableFuture<Integer> copy(int sourceGroupId, String name, PermissionGroupDatabaseType type) {
        return requests.createdId(ServerGroupCommands.serverGroupCopy(sourceGroupId, name, type), "sgid");
    }

    /** @see ServerGroupApi#overwrite(int, int, PermissionGroupDatabaseType) */
    public CompletableFuture<Void> overwrite(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
        if (targetGroupId <= 0)
            throw new IllegalArgumentException("Target group ID must be positive; use copy to create a group");
        return requests.run(ServerGroupCommands.serverGroupCopy(sourceGroupId, targetGroupId, type));
    }

    /** @see ServerGroupApi#rename(int, String) */
    public CompletableFuture<Void> rename(int groupId, String name) {
        return requests.run(ServerGroupCommands.serverGroupRename(groupId, name));
    }

    /** @see ServerGroupApi#delete(int) */
    public CompletableFuture<Void> delete(int groupId) {
        return delete(groupId, true);
    }

    /** @see ServerGroupApi#delete(int, boolean) */
    public CompletableFuture<Void> delete(int groupId, boolean force) {
        return requests.run(ServerGroupCommands.serverGroupDel(groupId, force));
    }

    /** @see ServerGroupApi#members(int) */
    public CompletableFuture<List<ServerGroupClient>> members(int groupId) {
        return requests.rows(ServerGroupCommands.serverGroupClientList(groupId), ServerGroupClient::from);
    }

    /** @see ServerGroupApi#addMember(int, ClientDatabaseId) */
    public CompletableFuture<Void> addMember(int groupId, ClientDatabaseId clientDatabaseId) {
        return requests.run(ServerGroupCommands.serverGroupAddClient(groupId, clientDatabaseId.value()));
    }

    /** @see ServerGroupApi#removeMember(int, ClientDatabaseId) */
    public CompletableFuture<Void> removeMember(int groupId, ClientDatabaseId clientDatabaseId) {
        return requests.run(ServerGroupCommands.serverGroupDelClient(groupId, clientDatabaseId.value()));
    }

    /** @see ServerGroupApi#ofClient(ClientDatabaseId) */
    public CompletableFuture<List<ServerGroup>> ofClient(ClientDatabaseId clientDatabaseId) {
        var ids = requests.integers(ServerGroupCommands.serverGroupsByClientId(clientDatabaseId.value()), "sgid");
        return QueryRequests.findByKey(ids, list(), ServerGroup::getId);
    }
}
