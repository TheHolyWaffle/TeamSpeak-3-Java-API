package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroupClient;
import java.util.List;

/**
 * Server group administration. Server group permissions are managed through
 * {@link PermissionApi} with {@link com.github.theholywaffle.teamspeak3.api.PermissionTarget#serverGroup(ServerGroupId)}.
 *
 * @see ServerGroupApiAsync the asynchronous form
 */
public final class ServerGroupApi {

    private final ServerGroupApiAsync async;

    ServerGroupApi(ServerGroupApiAsync async) {
        this.async = async;
    }

    /**
     * Lists all server groups, including query and template groups.
     *
     * @return the server groups
     */
    public List<ServerGroup> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * Creates a regular server group.
     *
     * @param name the group name
     * @return the ID of the new group
     */
    public ServerGroupId create(String name) {
        return CommandFutures.join(async.create(name));
    }

    /**
     * Creates a server group of the given type.
     *
     * @param name the group name
     * @param type the group type
     * @return the ID of the new group
     */
    public ServerGroupId create(String name, PermissionGroupDatabaseType type) {
        return CommandFutures.join(async.create(name, type));
    }

    /**
     * Copies a server group into a new group.
     *
     * @param sourceGroupId the group to copy
     * @param name the new group's name
     * @param type the new group's type
     * @return the ID of the new group
     */
    public ServerGroupId copy(ServerGroupId sourceGroupId, String name, PermissionGroupDatabaseType type) {
        return CommandFutures.join(async.copy(sourceGroupId, name, type));
    }

    /**
     * Overwrites an existing server group with a copy of another group's permissions.
     *
     * @param sourceGroupId the group to copy
     * @param targetGroupId the existing group to overwrite
     * @param type the target group's type
     */
    public void overwrite(ServerGroupId sourceGroupId, ServerGroupId targetGroupId, PermissionGroupDatabaseType type) {
        CommandFutures.join(async.overwrite(sourceGroupId, targetGroupId, type));
    }

    /**
     * Renames a server group.
     *
     * @param groupId the group ID
     * @param name the new name
     */
    public void rename(ServerGroupId groupId, String name) {
        CommandFutures.join(async.rename(groupId, name));
    }

    /**
     * Deletes a server group, even if it still has members.
     *
     * @param groupId the group ID
     */
    public void delete(ServerGroupId groupId) {
        CommandFutures.join(async.delete(groupId));
    }

    /**
     * Deletes a server group.
     *
     * @param groupId the group ID
     * @param force whether to delete a group that still has members
     */
    public void delete(ServerGroupId groupId, boolean force) {
        CommandFutures.join(async.delete(groupId, force));
    }

    /**
     * Lists the members of a server group.
     *
     * @param groupId the group ID
     * @return the members
     * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 2560 if the group does not exist
     */
    public List<ServerGroupClient> members(ServerGroupId groupId) {
        return CommandFutures.join(async.members(groupId));
    }

    /**
     * Adds a database client to a server group.
     *
     * @param groupId the group ID
     * @param clientDatabaseId the client database ID
     */
    public void addMember(ServerGroupId groupId, ClientDatabaseId clientDatabaseId) {
        CommandFutures.join(async.addMember(groupId, clientDatabaseId));
    }

    /**
     * Removes a database client from a server group.
     *
     * @param groupId the group ID
     * @param clientDatabaseId the client database ID
     */
    public void removeMember(ServerGroupId groupId, ClientDatabaseId clientDatabaseId) {
        CommandFutures.join(async.removeMember(groupId, clientDatabaseId));
    }

    /**
     * Lists the server groups of a database client.
     *
     * @param clientDatabaseId the client database ID
     * @return the client's server groups
     */
    public List<ServerGroup> ofClient(ClientDatabaseId clientDatabaseId) {
        return CommandFutures.join(async.ofClient(clientDatabaseId));
    }
}
