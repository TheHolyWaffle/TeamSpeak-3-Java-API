package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class ComplaintCommands {

    private ComplaintCommands() {
        throw new Error("No instances");
    }

    public static Command complainAdd(long clientDBId, String message) {
        CommandBuilder builder = new CommandBuilder("complainadd");
        builder.add(new KeyValueParam("tcldbid", clientDBId));
        builder.add(new KeyValueParam("message", message));
        return builder.build();
    }

    public static Command complainDel(long clientDBId, long fromClientDBId) {
        CommandBuilder builder = new CommandBuilder("complaindel");
        builder.add(new KeyValueParam("tcldbid", clientDBId));
        builder.add(new KeyValueParam("fcldbid", fromClientDBId));
        return builder.build();
    }

    public static Command complainDelAll(long clientDBId) {
        return new CommandBuilder("complaindelall")
                .add(new KeyValueParam("tcldbid", clientDBId))
                .build();
    }

    public static Command complainList(long clientDBId) {
        return new CommandBuilder("complainlist")
                .addIf(clientDBId > 0, new KeyValueParam("tcldbid", clientDBId))
                .build();
    }
}
