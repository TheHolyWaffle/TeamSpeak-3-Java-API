package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import java.util.List;

public final class PermissionCommands {

    private PermissionCommands() {
        throw new Error("No instances");
    }

    /*
     * General commands
     */

    public static Command permFind(String permName) {
        nonEmptyPermissionName(permName);

        return new CommandBuilder("permfind")
                .add(new KeyValueParam("permsid", permName))
                .build();
    }

    public static Command permGet(String... permNames) {
        return new CommandBuilder("permget").add(permissionNames(permNames)).build();
    }

    public static Command permIdGetByName(String... permNames) {
        return new CommandBuilder("permidgetbyname")
                .add(permissionNames(permNames))
                .build();
    }

    public static Command permissionList() {
        return new CommandBuilder("permissionlist").build();
    }

    public static Command permOverview(long channelId, long clientDBId) {
        CommandBuilder builder = new CommandBuilder("permoverview");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("permid", 0));
        return builder.build();
    }

    public static Command permReset() {
        return new CommandBuilder("permreset").build();
    }

    /*
     * Client commands
     */

    public static Command clientAddPerm(long clientDBId, String permName, int permValue, boolean skip) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("clientaddperm");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        builder.add(new KeyValueParam("permskip", skip));
        return builder.build();
    }

    public static Command clientDelPerm(long clientDBId, String permName) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("clientdelperm");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command clientPermList(long clientDBId) {
        CommandBuilder builder = new CommandBuilder("clientpermlist");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new OptionParam("permsid"));
        return builder.build();
    }

    /*
     * Channel commands
     */

    public static Command channelAddPerm(long channelId, String permName, int permValue) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channeladdperm");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        return builder.build();
    }

    public static Command channelDelPerm(long channelId, String permName) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channeldelperm");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command channelPermList(long channelId) {
        CommandBuilder builder = new CommandBuilder("channelpermlist");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new OptionParam("permsid"));
        return builder.build();
    }

    /*
     * Channel client commands
     */

    public static Command channelClientAddPerm(long channelId, long clientDBId, String permName, int permValue) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channelclientaddperm");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        return builder.build();
    }

    public static Command channelClientDelPerm(long channelId, long clientDBId, String permName) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channelclientdelperm");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command channelClientPermList(long channelId, long clientDBId) {
        CommandBuilder builder = new CommandBuilder("channelclientpermlist");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new OptionParam("permsid"));
        return builder.build();
    }

    /*
     * Channel group commands
     */

    public static Command channelGroupAddPerm(long groupId, String permName, int permValue) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channelgroupaddperm");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        return builder.build();
    }

    public static Command channelGroupDelPerm(long groupId, String permName) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("channelgroupdelperm");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command channelGroupPermList(long groupId) {
        CommandBuilder builder = new CommandBuilder("channelgrouppermlist");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new OptionParam("permsid"));
        return builder.build();
    }

    /*
     * Server group commands
     */

    public static Command serverGroupAddPerm(
            long groupId, String permName, int permValue, boolean negate, boolean skip) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("servergroupaddperm");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        builder.add(new KeyValueParam("permnegated", negate));
        builder.add(new KeyValueParam("permskip", skip));
        return builder.build();
    }

    public static Command serverGroupAutoAddPerm(
            ServerGroupType type, String permName, int permValue, boolean negate, boolean skip) {
        nonNullServerGroupType(type);
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("servergroupautoaddperm");
        builder.add(new KeyValueParam("sgtype", type.getIndex()));
        builder.add(new KeyValueParam("permsid", permName));
        builder.add(new KeyValueParam("permvalue", permValue));
        builder.add(new KeyValueParam("permnegated", negate));
        builder.add(new KeyValueParam("permskip", skip));
        return builder.build();
    }

    public static Command serverGroupDelPerm(long groupId, String permName) {
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("servergroupdelperm");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command serverGroupAutoDelPerm(ServerGroupType type, String permName) {
        nonNullServerGroupType(type);
        nonEmptyPermissionName(permName);

        CommandBuilder builder = new CommandBuilder("servergroupautodelperm");
        builder.add(new KeyValueParam("sgtype", type.getIndex()));
        builder.add(new KeyValueParam("permsid", permName));
        return builder.build();
    }

    public static Command serverGroupPermList(long groupId) {
        CommandBuilder builder = new CommandBuilder("servergrouppermlist");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new OptionParam("permsid"));
        return builder.build();
    }

    /*
     * Validation
     */

    private static void nonEmptyPermissionName(String permName) {
        if (permName == null || permName.isEmpty()) {
            throw new IllegalArgumentException("Permission name must be a non-empty string");
        }
    }

    private static ArrayParameter permissionNames(String[] permNames) {
        if (permNames == null || permNames.length == 0) {
            throw new IllegalArgumentException("Permission name array cannot be null or empty");
        }
        for (String permName : permNames) nonEmptyPermissionName(permName);
        return ArrayParameter.of("permsid", List.of(permNames));
    }

    private static void nonNullServerGroupType(ServerGroupType type) {
        if (type == null) {
            throw new IllegalArgumentException("Server group type cannot be null");
        }
    }
}
