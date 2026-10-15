package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class MessageCommands {

    private MessageCommands() {
        throw new Error("No instances");
    }

    public static Command messageAdd(String clientUId, String subject, String message) {
        if (clientUId == null || clientUId.isEmpty()) {
            throw new IllegalArgumentException("Client UId must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("messageadd");
        builder.add(new KeyValueParam("cluid", clientUId));
        builder.add(new KeyValueParam("subject", subject));
        builder.add(new KeyValueParam("message", message));
        return builder.build();
    }

    public static Command messageDel(long messageId) {
        return new CommandBuilder("messagedel")
                .add(new KeyValueParam("msgid", messageId))
                .build();
    }

    public static Command messageGet(long messageId) {
        return new CommandBuilder("messageget")
                .add(new KeyValueParam("msgid", messageId))
                .build();
    }

    public static Command messageList() {
        return new CommandBuilder("messagelist").build();
    }

    public static Command messageUpdateFlag(long messageId, boolean read) {
        CommandBuilder builder = new CommandBuilder("messageupdateflag");
        builder.add(new KeyValueParam("msgid", messageId));
        builder.add(new KeyValueParam("flag", read));
        return builder.build();
    }
}
