package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import java.util.Optional;

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

    public static Command channelGroupClientList(ChannelGroupFilter filter) {
        return new CommandBuilder("channelgroupclientlist")
                .addIfPresent("cid", filter.channelId())
                .addIfPresent("cldbid", filter.clientDatabaseId())
                .addIfPresent("cgid", filter.groupId())
                .build();
    }

    public static Command channelGroupCopy(
            ChannelGroupId sourceGroupId, ChannelGroupId targetGroupId, PermissionGroupDatabaseType type) {
        return channelGroupCopy(sourceGroupId, Optional.of(targetGroupId), "name", type);
    }

    public static Command channelGroupCopy(
            ChannelGroupId sourceGroupId, String groupName, PermissionGroupDatabaseType type) {
        return channelGroupCopy(sourceGroupId, Optional.empty(), groupName, type);
    }

    private static Command channelGroupCopy(
            ChannelGroupId sourceGroupId,
            Optional<ChannelGroupId> targetGroupId,
            String groupName,
            PermissionGroupDatabaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Group type cannot be null");
        }
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Channel group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelgroupcopy");
        builder.add(new KeyValueParam("scgid", sourceGroupId));
        builder.add(KeyValueParam.idOrZero("tcgid", targetGroupId));
        builder.add(new KeyValueParam("name", groupName));
        builder.add(new KeyValueParam("type", KnownCodes.require(type.getIndex())));
        return builder.build();
    }

    public static Command channelGroupDel(ChannelGroupId channelGroupId, boolean forced) {
        CommandBuilder builder = new CommandBuilder("channelgroupdel");
        builder.add(new KeyValueParam("cgid", channelGroupId));
        builder.add(new KeyValueParam("force", forced));
        return builder.build();
    }

    public static Command channelGroupList() {
        return new CommandBuilder("channelgrouplist").build();
    }

    public static Command channelGroupRename(ChannelGroupId groupId, String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Channel group name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelgrouprename");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("name", groupName));
        return builder.build();
    }

    public static Command setClientChannelGroup(
            ChannelGroupId groupId, ChannelId channelId, ClientDatabaseId clientDBId) {
        CommandBuilder builder = new CommandBuilder("setclientchannelgroup");
        builder.add(new KeyValueParam("cgid", groupId));
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cldbid", clientDBId));
        return builder.build();
    }
}
