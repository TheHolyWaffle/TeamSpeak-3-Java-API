package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class CustomPropertyCommands {

    private CustomPropertyCommands() {
        throw new Error("No instances");
    }

    public static Command customDelete(ClientDatabaseId clientDBId, String key) {
        CommandBuilder builder = new CommandBuilder("customdelete");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("ident", key));
        return builder.build();
    }

    public static Command customInfo(ClientDatabaseId clientDBId) {
        return new CommandBuilder("custominfo")
                .add(new KeyValueParam("cldbid", clientDBId))
                .build();
    }

    public static Command customSet(ClientDatabaseId clientDBId, String key, String value) {
        CommandBuilder builder = new CommandBuilder("customset");
        builder.add(new KeyValueParam("cldbid", clientDBId));
        builder.add(new KeyValueParam("ident", key));
        builder.add(new KeyValueParam("value", value));
        return builder.build();
    }

    public static Command customSearch(String key, String pattern) {
        CommandBuilder builder = new CommandBuilder("customsearch");
        builder.add(new KeyValueParam("ident", key));
        builder.add(new KeyValueParam("pattern", pattern));
        return builder.build();
    }
}
