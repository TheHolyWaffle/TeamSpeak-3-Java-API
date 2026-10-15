package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import java.util.Optional;

public final class QueryLoginCommands {

    private QueryLoginCommands() {
        throw new Error("No instances");
    }

    /** @param clientDBId the owning client, or empty for the invoker */
    public static Command queryLoginAdd(String loginName, Optional<ClientDatabaseId> clientDBId) {
        CommandBuilder builder = new CommandBuilder("queryloginadd");
        builder.add(new KeyValueParam("client_login_name", loginName));
        builder.addIfPresent("cldbid", clientDBId);
        return builder.build();
    }

    public static Command queryLoginDel(ClientDatabaseId clientDBId) {
        return new CommandBuilder("querylogindel")
                .add(new KeyValueParam("cldbid", clientDBId))
                .build();
    }

    public static Command queryLoginList(String pattern) {
        CommandBuilder builder = new CommandBuilder("queryloginlist");
        builder.addIf(pattern != null, new KeyValueParam("pattern", pattern));
        return builder.build();
    }
}
