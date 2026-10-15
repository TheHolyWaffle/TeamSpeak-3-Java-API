package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import java.util.Optional;

public final class ComplaintCommands {

    private ComplaintCommands() {
        throw new Error("No instances");
    }

    public static Command complainAdd(ClientDatabaseId clientDBId, String message) {
        CommandBuilder builder = new CommandBuilder("complainadd");
        builder.add(new KeyValueParam("tcldbid", clientDBId));
        builder.add(new KeyValueParam("message", message));
        return builder.build();
    }

    public static Command complainDel(ClientDatabaseId clientDBId, ClientDatabaseId fromClientDBId) {
        CommandBuilder builder = new CommandBuilder("complaindel");
        builder.add(new KeyValueParam("tcldbid", clientDBId));
        builder.add(new KeyValueParam("fcldbid", fromClientDBId));
        return builder.build();
    }

    public static Command complainDelAll(ClientDatabaseId clientDBId) {
        return new CommandBuilder("complaindelall")
                .add(new KeyValueParam("tcldbid", clientDBId))
                .build();
    }

    /** @param clientDBId the target client, or empty for all complaints */
    public static Command complainList(Optional<ClientDatabaseId> clientDBId) {
        return new CommandBuilder("complainlist")
                .addIfPresent("tcldbid", clientDBId)
                .build();
    }
}
