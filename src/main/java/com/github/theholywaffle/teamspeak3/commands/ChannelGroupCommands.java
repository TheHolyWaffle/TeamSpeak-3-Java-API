package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class ChannelGroupCommands {

    private ChannelGroupCommands() {
        throw new Error("No instances");
    }

    public static Command channelGroupAdd(String groupName, PermissionGroupDatabaseType type) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Channel group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelgroupadd");
        builder.add(new KeyValueParam("name", groupName));
        if (type != null) {
            builder.add(new KeyValueParam("type", KnownCodes.require(type.getIndex())));
        }
        return builder.build();
    }

    public static Command channelGroupClientList(int channelId, int clientDBId, int groupId) {
        CommandBuilder builder = new CommandBuilder("channelgroupclientlist");
        builder.addIf(channelId > 0, new KeyValueParam("cid", channelId));
        builder.addIf(clientDBId > 0, new KeyValueParam("cldbid", clientDBId));
        builder.addIf(groupId > 0, new KeyValueParam("cgid", groupId));
        return builder.build();
    }

    public static Command channelGroupCopy(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType type) {
        return channelGroupCopy(sourceGroupId, targetGroupId, "name", type);
    }

    public static Command channelGroupCopy(int sourceGroupId, String groupName, PermissionGroupDatabaseType type) {
        return channelGroupCopy(sourceGroupId, 0, groupName, type);
    }

    private static Command channelGroupCopy(
            int sourceGroupId, int targetGroupId, String groupName, PermissionGroupDatabaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Group type cannot be null");
        }
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Channel group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelgroupcopy");
        builder.add(new KeyValueParam("scgid", sourceGroupId));
        builder.add(new KeyValueParam("tcgid", targetGroupId));
        builder.add(new KeyValueParam("name", groupName));
        builder.add(new KeyValueParam("type", KnownCodes.require(type.getIndex())));
        return builder.build();
    }

    public static Command channelGroupDel(int channelGroupId, boolean forced) {
        CommandBuilder builder = new CommandBuilder("channelgroupdel");
        builder.add(new KeyValueParam("cgid", channelGroupId));
        builder.add(new KeyValueParam("force", forced));
        return builder.build();
    }

    public static Command channelGroupList() {
        return new CommandBuilder("channelgrouplist").build();
    }

    public static Command channelGroupRename(int groupId, String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Channel group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelgrouprename");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("name", groupName));
        return builder.build();
    }

    public static Command setClientChannelGroup(int groupId, int channelId, int clientDBId) {
        CommandBuilder builder = new CommandBuilder("setclientchannelgroup");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        return builder.build();
    }
}
