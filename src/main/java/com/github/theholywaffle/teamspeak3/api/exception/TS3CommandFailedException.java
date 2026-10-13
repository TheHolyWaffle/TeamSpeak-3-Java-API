package com.github.theholywaffle.teamspeak3.api.exception;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;

/** The server answered a command with an error; the command was not executed. */
public class TS3CommandFailedException extends TS3Exception {

    private static final long serialVersionUID = 8179203326662268882L;

    private final QueryError queryError;
    private final String commandName;

    public TS3CommandFailedException(QueryError error, String commandName) {
        super(buildMessage(error, commandName));
        queryError = error;
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
        return queryError;
    }

    /** @return the server error ID, for example 768 for an invalid channel ID */
    public int getErrorId() {
        return queryError.getId();
    }

    /** @return the protocol command name, excluding parameters */
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean matchesQueryError(int errorId) {
        return queryError.getId() == errorId;
    }
}
