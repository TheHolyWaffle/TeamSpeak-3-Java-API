package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant.ChannelGroupGrant;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant.ServerGroupGrant;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class PrivilegeKeyCommands {

    private PrivilegeKeyCommands() {
        throw new Error("No instances");
    }

    public static Command privilegeKeyAdd(PrivilegeKeyGrant grant, String description) {
        CommandBuilder builder = new CommandBuilder("privilegekeyadd");
        builder.add(
                new KeyValueParam("tokentype", KnownCodes.require(grant.type().getIndex())));
        switch (grant) {
            case ServerGroupGrant(var group) -> {
                builder.add(new KeyValueParam("tokenid1", group));
                builder.add(new KeyValueParam("tokenid2", 0));
            }
            case ChannelGroupGrant(var group, var channel) -> {
                builder.add(new KeyValueParam("tokenid1", group));
                builder.add(new KeyValueParam("tokenid2", channel));
            }
        }
        builder.addIf(description != null, new KeyValueParam("tokendescription", description));
        return builder.build();
    }

    public static Command privilegeKeyDelete(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Privilege key must be a non-empty string");
        }

        return new CommandBuilder("privilegekeydelete")
                .add(new KeyValueParam("token", token))
                .build();
    }

    public static Command privilegeKeyList() {
        return new CommandBuilder("privilegekeylist").build();
    }

    public static Command privilegeKeyUse(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Privilege key must be a non-empty string");
        }

        return new CommandBuilder("privilegekeyuse")
                .add(new KeyValueParam("token", token))
                .build();
    }
}
