package com.github.theholywaffle.teamspeak3.api.exception;

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import java.util.LinkedHashMap;

/** The server answered a command with an error; the command was not executed. */
public class TS3CommandFailedException extends TS3Exception {

    private static final long serialVersionUID = 8179203326662268882L;

    // QueryError is not serializable; its properties are.
    private final LinkedHashMap<String, String> error;
    private final String commandName;

    public TS3CommandFailedException(QueryError error, String commandName) {
        super(buildMessage(error, commandName));
        this.error = new LinkedHashMap<>(error.getMap());
        this.commandName = commandName;
    }

    private static String buildMessage(QueryError error, String cmdName) {
        final StringBuilder msg = new StringBuilder();
        msg.append("A \"").append(cmdName).append("\" command returned with a server error.\n");
        msg.append(">> ")
                .append(error.find("msg").orElse(""))
                .append(" (ID ")
                .append(error.getId())
                .append(')');
        error.getExtraMessage().ifPresent(extra -> msg.append(": ").append(extra));
        error.getFailedPermissionId()
                .ifPresent(id -> msg.append(", failed permission with ID ").append(id));

        return msg.toString();
    }

    public QueryError getError() {
        return new QueryError(error);
    }

    /** @return the server error ID, for example 768 for an invalid channel ID */
    public int getErrorId() {
        return getError().getId();
    }

    /** @return the protocol command name, excluding parameters */
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean matchesQueryError(int errorId) {
        return getErrorId() == errorId;
    }
}
