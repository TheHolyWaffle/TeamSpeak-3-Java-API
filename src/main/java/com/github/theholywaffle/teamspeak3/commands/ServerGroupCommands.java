package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import java.util.Optional;

public final class ServerGroupCommands {

    private ServerGroupCommands() {
        throw new Error("No instances");
    }

    public static Command serverGroupAdd(String groupName, PermissionGroupDatabaseType type) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Server group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("servergroupadd");
        builder.add(new KeyValueParam("name", groupName));
        if (type != null) {
            builder.add(new KeyValueParam("type", KnownCodes.require(type.getIndex())));
        }
        return builder.build();
    }

    public static Command serverGroupAddClient(ServerGroupId groupId, ClientDatabaseId clientDBId) {
        CommandBuilder builder = new CommandBuilder("servergroupaddclient");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        return builder.build();
    }

    public static Command serverGroupClientList(ServerGroupId groupId) {
        CommandBuilder builder = new CommandBuilder("servergroupclientlist");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new OptionParam("names"));
        return builder.build();
    }

    public static Command serverGroupCopy(
            ServerGroupId sourceGroupId, ServerGroupId targetGroupId, PermissionGroupDatabaseType type) {
        return serverGroupCopy(sourceGroupId, Optional.of(targetGroupId), "name", type);
    }

    public static Command serverGroupCopy(
            ServerGroupId sourceGroupId, String groupName, PermissionGroupDatabaseType type) {
        return serverGroupCopy(sourceGroupId, Optional.empty(), groupName, type);
    }

    private static Command serverGroupCopy(
            ServerGroupId sourceGroupId,
            Optional<ServerGroupId> targetGroupId,
            String groupName,
            PermissionGroupDatabaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Group type cannot be null");
        }
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Server group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("servergroupcopy");
        builder.add(new KeyValueParam("ssgid", sourceGroupId));
        builder.add(KeyValueParam.idOrZero("tsgid", targetGroupId));
        builder.add(new KeyValueParam("name", groupName));
        builder.add(new KeyValueParam("type", KnownCodes.require(type.getIndex())));
        return builder.build();
    }

    public static Command serverGroupDel(ServerGroupId id, boolean force) {
        CommandBuilder builder = new CommandBuilder("servergroupdel");
        builder.add(new KeyValueParam("sgid", id));
        builder.add(new KeyValueParam("force", force));
        return builder.build();
    }

    public static Command serverGroupDelClient(ServerGroupId groupId, ClientDatabaseId clientDBId) {
        CommandBuilder builder = new CommandBuilder("servergroupdelclient");
        builder.add(new KeyValueParam("sgid", groupId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        return builder.build();
    }

    public static Command serverGroupList() {
        return new CommandBuilder("servergrouplist").build();
    }

    public static Command serverGroupRename(ServerGroupId id, String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Server group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("servergrouprename");
        builder.add(new KeyValueParam("sgid", id));
        builder.add(new KeyValueParam("name", groupName));
        return builder.build();
    }

    public static Command serverGroupsByClientId(ClientDatabaseId clientDBId) {
        return new CommandBuilder("servergroupsbyclientid")
                .add(new KeyValueParam("cldbid", clientDBId))
                .build();
    }
}
