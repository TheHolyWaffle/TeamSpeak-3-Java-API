package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import java.util.Map;
import java.util.Optional;

public final class ChannelCommands {

    private ChannelCommands() {
        throw new Error("No instances");
    }

    public static Command channelCreate(String name, Map<ChannelProperty, String> options) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Channel name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("channelcreate");
        builder.add(new KeyValueParam("channel_name", name));
        builder.addProperties(options);
        return builder.build();
    }

    public static Command channelDelete(ChannelId channelId, boolean force) {
        CommandBuilder builder = new CommandBuilder("channeldelete");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("force", force));
        return builder.build();
    }

    public static Command channelEdit(ChannelId channelId, Map<ChannelProperty, String> options) {
        CommandBuilder builder = new CommandBuilder("channeledit");
        builder.add(new KeyValueParam("cid", channelId));
        builder.addProperties(options);
        return builder.build();
    }

    public static Command channelFind(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Channel name pattern must be a non-empty string");
        }

        return new CommandBuilder("channelfind")
                .add(new KeyValueParam("pattern", pattern))
                .build();
    }

    public static Command channelInfo(ChannelId channelId) {
        return new CommandBuilder("channelinfo")
                .add(new KeyValueParam("cid", channelId))
                .build();
    }

    public static Command channelList() {
        CommandBuilder builder = new CommandBuilder("channellist");
        builder.add(new OptionParam("topic"));
        builder.add(new OptionParam("flags"));
        builder.add(new OptionParam("voice"));
        builder.add(new OptionParam("limits"));
        builder.add(new OptionParam("icon"));
        builder.add(new OptionParam("secondsempty"));
        builder.add(new OptionParam("banners"));
        return builder.build();
    }

    /**
     * @param parentChannelId the new parent, or empty for the top level
     * @param afterChannelId the sibling to sort after, or empty to sort first
     */
    public static Command channelMove(
            ChannelId channelId, Optional<ChannelId> parentChannelId, Optional<ChannelId> afterChannelId) {
        CommandBuilder builder = new CommandBuilder("channelmove");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(KeyValueParam.idOrZero("cpid", parentChannelId));
        builder.add(KeyValueParam.idOrZero("order", afterChannelId));
        return builder.build();
    }
}
