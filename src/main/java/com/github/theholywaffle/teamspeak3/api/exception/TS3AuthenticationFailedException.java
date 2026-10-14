package com.github.theholywaffle.teamspeak3.api.exception;

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;

/**
 * The server rejected the credentials of a Web Query request with HTTP 401 or 403: the API key is
 * missing, invalid or expired, or its scope does not allow the command. The command was not executed.
 * {@link #getErrorId()} distinguishes the cause, for example 5122 (invalid key) or 5120 (out of scope).
 */
public class TS3AuthenticationFailedException extends TS3CommandFailedException {

    private static final long serialVersionUID = 1L;

    public TS3AuthenticationFailedException(QueryError error, String commandName) {
        super(error, commandName);
    }
}
