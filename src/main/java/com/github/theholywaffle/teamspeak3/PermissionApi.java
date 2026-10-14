package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.PermissionGrant;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionInfo;
import java.util.List;

/**
 * Permission metadata and assignments.
 * <p>
 * One set of methods covers every permission holder; describe the holder with
 * {@link com.github.theholywaffle.teamspeak3.api.PermissionTarget} and the assignment with
 * {@link com.github.theholywaffle.teamspeak3.api.PermissionGrant}. An unknown permission name fails with
 * server error 2562.
 * </p>
 *
 * @see PermissionApiAsync the asynchronous form
 */
public final class PermissionApi {

    private final PermissionApiAsync async;

    PermissionApi(PermissionApiAsync async) {
        this.async = async;
    }

    /**
     * Lists every permission known to the server.
     *
     * @return the permission definitions
     */
    public List<PermissionInfo> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * @param permissionName the permission name
     * @return the server's numeric ID for this permission
     */
    public int idByName(String permissionName) {
        return CommandFutures.join(async.idByName(permissionName));
    }

    /**
     * @param permissionNames the permission names
     * @return the numeric IDs, in the order of the names
     */
    public List<Integer> idsByName(String... permissionNames) {
        return CommandFutures.join(async.idsByName(permissionNames));
    }

    /**
     * Gets the value of a permission for this query client.
     *
     * @param permissionName the permission name
     * @return the permission value
     */
    public int ownValue(String permissionName) {
        return CommandFutures.join(async.ownValue(permissionName));
    }

    /**
     * Gets the values of several permissions for this query client.
     *
     * @param permissionNames the permission names
     * @return the values, in the order of the names
     */
    public List<Integer> ownValues(String... permissionNames) {
        return CommandFutures.join(async.ownValues(permissionNames));
    }

    /**
     * Lists every holder that has a permission assigned.
     *
     * @param permissionName the permission name
     * @return the assignments; empty if no holder has the permission
     */
    public List<PermissionAssignment> assignments(String permissionName) {
        return CommandFutures.join(async.assignments(permissionName));
    }

    /**
     * Lists every permission assignment that applies to a database client in a channel.
     *
     * @param channelId the channel ID
     * @param clientDatabaseId the client database ID
     * @return the effective assignments
     */
    public List<PermissionAssignment> overview(int channelId, ClientDatabaseId clientDatabaseId) {
        return CommandFutures.join(async.overview(channelId, clientDatabaseId));
    }

    /**
     * Restores the default permission settings of the selected virtual server and creates a new administrator privilege key.
     *
     * @return the new administrator privilege key
     */
    public String reset() {
        return CommandFutures.join(async.reset());
    }

    /**
     * Lists the permissions assigned directly to a holder.
     *
     * @param target the permission holder
     * @return the holder's permissions; empty if none are assigned
     */
    public List<Permission> assigned(PermissionTarget target) {
        return CommandFutures.join(async.assigned(target));
    }

    /**
     * Adds or replaces a permission of a holder without the negated or skip flag.
     *
     * @param target the permission holder
     * @param permissionName the permission name
     * @param value the permission value; boolean permissions use 1 or 0
     */
    public void add(PermissionTarget target, String permissionName, int value) {
        CommandFutures.join(async.add(target, permissionName, value));
    }

    /**
     * Adds or replaces a permission of a holder.
     *
     * @param target the permission holder
     * @param grant the permission and its value
     * @throws IllegalArgumentException if the holder does not accept the grant's negated or skip flag
     */
    public void add(PermissionTarget target, PermissionGrant grant) {
        CommandFutures.join(async.add(target, grant));
    }

    /**
     * Removes a permission from a holder.
     *
     * @param target the permission holder
     * @param permissionName the permission name
     */
    public void delete(PermissionTarget target, String permissionName) {
        CommandFutures.join(async.delete(target, permissionName));
    }

    /**
     * Adds or replaces a permission in every server group of a type.
     *
     * @param type the server group type
     * @param grant the permission and its value
     */
    public void addToAllServerGroups(ServerGroupType type, PermissionGrant grant) {
        CommandFutures.join(async.addToAllServerGroups(type, grant));
    }

    /**
     * Removes a permission from every server group of a type.
     *
     * @param type the server group type
     * @param permissionName the permission name
     */
    public void deleteFromAllServerGroups(ServerGroupType type, String permissionName) {
        CommandFutures.join(async.deleteFromAllServerGroups(type, permissionName));
    }
}
