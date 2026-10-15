package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/** The transport ended after a command entered the write phase; the server may have applied it. */
public class TS3UnknownOutcomeException extends TS3QueryShutDownException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String commandName;

    /** @param commandName protocol command name, excluding parameters and credentials */
    public TS3UnknownOutcomeException(String commandName) {
        this.commandName = commandName;
    }

    /** @return the command name whose outcome is unknown */
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getMessage() {
        return "Outcome of " + commandName + " is unknown; it may have executed on the server";
    }

    @Override
    public boolean isOutcomeUnknown() {
        return true;
    }
}
