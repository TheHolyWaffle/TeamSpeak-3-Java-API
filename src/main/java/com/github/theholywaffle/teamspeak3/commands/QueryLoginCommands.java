package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class QueryLoginCommands {

    private QueryLoginCommands() {
        throw new Error("No instances");
    }

    public static Command queryLoginAdd(String loginName, long clientDBId) {
        CommandBuilder builder = new CommandBuilder("queryloginadd");
        builder.add(new KeyValueParam("client_login_name", loginName));
        builder.addIf(clientDBId > 0, new KeyValueParam("cldbid", clientDBId));
        return builder.build();
    }

    public static Command queryLoginDel(long clientDBId) {
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
