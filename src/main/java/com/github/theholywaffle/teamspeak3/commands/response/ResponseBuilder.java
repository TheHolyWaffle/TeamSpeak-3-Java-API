package com.github.theholywaffle.teamspeak3.commands.response;

import com.github.theholywaffle.teamspeak3.commands.Command;
import java.util.StringJoiner;

public class ResponseBuilder {

    private final Command command;
    private final StringJoiner rows = new StringJoiner("|");

    public ResponseBuilder(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public DefaultArrayResponse buildResponse() {
        return DefaultArrayResponse.parse(rows.toString());
    }

    public void appendResponse(String rawArrayResponse) {
        rows.add(rawArrayResponse);
    }
}
