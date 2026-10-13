package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionInfo;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.PermissionCommands;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous permission metadata and assignments for every permission holder.
 *
 * @see PermissionApi the synchronous form and full documentation
 */
public final class PermissionApiAsync {

    private final QueryRequests requests;

    PermissionApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see PermissionApi#list() */
    public CompletableFuture<List<PermissionInfo>> list() {
        return requests.rows(PermissionCommands.permissionList(), PermissionInfo::from);
    }

    /** @see PermissionApi#idByName(String) */
    public CompletableFuture<Integer> idByName(String permissionName) {
        return requests.integer(PermissionCommands.permIdGetByName(permissionName), "permid");
    }

    /** @see PermissionApi#idsByName(String...) */
    public CompletableFuture<List<Integer>> idsByName(String... permissionNames) {
        return requests.integers(PermissionCommands.permIdGetByName(permissionNames), "permid");
    }

    /** @see PermissionApi#ownValue(String) */
    public CompletableFuture<Integer> ownValue(String permissionName) {
        return requests.integer(PermissionCommands.permGet(permissionName), "permvalue");
    }

    /** @see PermissionApi#ownValues(String...) */
    public CompletableFuture<List<Integer>> ownValues(String... permissionNames) {
        return requests.integers(PermissionCommands.permGet(permissionNames), "permvalue");
    }

    /** @see PermissionApi#assignments(String) */
    public CompletableFuture<List<PermissionAssignment>> assignments(String permissionName) {
        return requests.models(PermissionCommands.permFind(permissionName), PermissionAssignment::new);
    }

    /** @see PermissionApi#overview(int, ClientDatabaseId) */
    public CompletableFuture<List<PermissionAssignment>> overview(int channelId, ClientDatabaseId clientDatabaseId) {
        return requests.models(
                PermissionCommands.permOverview(channelId, clientDatabaseId.value()), PermissionAssignment::new);
    }

    /** @see PermissionApi#reset() */
    public CompletableFuture<String> reset() {
        return requests.string(PermissionCommands.permReset(), "token");
    }

    /** @see PermissionApi#assigned(PermissionTarget) */
    public CompletableFuture<List<Permission>> assigned(PermissionTarget target) {
        Command command = switch (target.type()) {
            case SERVER_GROUP -> PermissionCommands.serverGroupPermList(target.id());
            case CHANNEL_GROUP -> PermissionCommands.channelGroupPermList(target.id());
            case CHANNEL -> PermissionCommands.channelPermList(target.id());
            case GLOBAL_CLIENT -> PermissionCommands.clientPermList(target.id());
            case CHANNEL_CLIENT -> PermissionCommands.channelClientPermList(target.id(), target.clientDatabaseId());
            case UNKNOWN -> throw new IllegalArgumentException("Unknown permission holder type");
        };
        return requests.rows(command, Permission::from);
    }

    /** @see PermissionApi#add(PermissionTarget, String, int) */
    public CompletableFuture<Void> add(PermissionTarget target, String permissionName, int value) {
        return add(target, PermissionGrant.of(permissionName, value));
    }

    /** @see PermissionApi#add(PermissionTarget, PermissionGrant) */
    public CompletableFuture<Void> add(PermissionTarget target, PermissionGrant grant) {
        grant.requireSupportedBy(target.type());
        Command command = switch (target.type()) {
            case SERVER_GROUP ->
                PermissionCommands.serverGroupAddPerm(
                        target.id(), grant.permissionName(), grant.value(), grant.negated(), grant.skipped());
            case CHANNEL_GROUP ->
                PermissionCommands.channelGroupAddPerm(target.id(), grant.permissionName(), grant.value());
            case CHANNEL -> PermissionCommands.channelAddPerm(target.id(), grant.permissionName(), grant.value());
            case GLOBAL_CLIENT ->
                PermissionCommands.clientAddPerm(target.id(), grant.permissionName(), grant.value(), grant.skipped());
            case CHANNEL_CLIENT ->
                PermissionCommands.channelClientAddPerm(
                        target.id(), target.clientDatabaseId(), grant.permissionName(), grant.value());
            case UNKNOWN -> throw new IllegalArgumentException("Unknown permission holder type");
        };
        return requests.run(command);
    }

    /** @see PermissionApi#delete(PermissionTarget, String) */
    public CompletableFuture<Void> delete(PermissionTarget target, String permissionName) {
        Command command = switch (target.type()) {
            case SERVER_GROUP -> PermissionCommands.serverGroupDelPerm(target.id(), permissionName);
            case CHANNEL_GROUP -> PermissionCommands.channelGroupDelPerm(target.id(), permissionName);
            case CHANNEL -> PermissionCommands.channelDelPerm(target.id(), permissionName);
            case GLOBAL_CLIENT -> PermissionCommands.clientDelPerm(target.id(), permissionName);
            case CHANNEL_CLIENT ->
                PermissionCommands.channelClientDelPerm(target.id(), target.clientDatabaseId(), permissionName);
            case UNKNOWN -> throw new IllegalArgumentException("Unknown permission holder type");
        };
        return requests.run(command);
    }

    /** @see PermissionApi#addToAllServerGroups(ServerGroupType, PermissionGrant) */
    public CompletableFuture<Void> addToAllServerGroups(ServerGroupType type, PermissionGrant grant) {
        grant.requireSupportedBy(PermissionGroupType.SERVER_GROUP);
        return requests.run(PermissionCommands.serverGroupAutoAddPerm(
                type, grant.permissionName(), grant.value(), grant.negated(), grant.skipped()));
    }

    /** @see PermissionApi#deleteFromAllServerGroups(ServerGroupType, String) */
    public CompletableFuture<Void> deleteFromAllServerGroups(ServerGroupType type, String permissionName) {
        return requests.run(PermissionCommands.serverGroupAutoDelPerm(type, permissionName));
    }
}
