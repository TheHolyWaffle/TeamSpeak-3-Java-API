package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class PrivilegeKeyCommands {

    private PrivilegeKeyCommands() {
        throw new Error("No instances");
    }

    public static Command privilegeKeyAdd(PrivilegeKeyType type, long groupId, long channelId, String description) {
        if (type == null) {
            throw new IllegalArgumentException("Privilege key type cannot be null");
        }

        CommandBuilder builder = new CommandBuilder("privilegekeyadd");
        builder.add(new KeyValueParam("tokentype", KnownCodes.require(type.getIndex())));
        builder.add(new KeyValueParam("tokenid1", groupId));
        builder.add(new KeyValueParam("tokenid2", channelId));
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
