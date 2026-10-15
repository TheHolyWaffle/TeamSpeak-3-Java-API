package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import java.util.Map;

public final class DatabaseClientCommands {

    private DatabaseClientCommands() {
        throw new Error("No instances");
    }

    public static Command clientDBDelete(ClientDatabaseId clientDBId) {
        return new CommandBuilder("clientdbdelete")
                .add(new KeyValueParam("cldbid", clientDBId))
                .build();
    }

    public static Command clientDBEdit(ClientDatabaseId clientDBId, Map<ClientProperty, String> options) {
        CommandBuilder builder = new CommandBuilder("clientdbedit");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.addProperties(options);
        return builder.build();
    }

    public static Command clientDBFind(String pattern, boolean uid) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("clientdbfind");
        builder.add(new KeyValueParam("pattern", pattern));
        builder.addIf(uid, new OptionParam("uid"));
        return builder.build();
    }

    public static Command clientDBInfo(ClientDatabaseId clientDBId) {
        return new CommandBuilder("clientdbinfo")
                .add(new KeyValueParam("cldbid", clientDBId))
                .build();
    }

    public static Command clientDBList(int begin, int amount, boolean count) {
        CommandBuilder builder = new CommandBuilder("clientdblist");
        builder.add(new KeyValueParam("start", begin));
        builder.add(new KeyValueParam("duration", amount));
        builder.addIf(count, new OptionParam("count"));
        return builder.build();
    }
}
