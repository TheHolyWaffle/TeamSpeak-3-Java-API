package com.github.theholywaffle.teamspeak3.api.exception;

/**
 * A Web Query response carried no usable ServerQuery status: an unexpected HTTP status, for example
 * from a reverse proxy, or a body that is not the documented JSON. A 4xx status means the server did
 * not execute the command; for any other status the outcome is unknown.
 */
public class TS3HttpException extends TS3TransportException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String commandName;

    /**
     * @param commandName protocol command name, excluding parameters and credentials
     * @param statusCode the HTTP status code
     * @param problem what was wrong with the response, without its content
     * @param cause the parsing failure, or {@code null}
     */
    public TS3HttpException(String commandName, int statusCode, String problem, Throwable cause) {
        super("Web Query command " + commandName + " returned HTTP " + statusCode + ": " + problem, cause);
        this.statusCode = statusCode;
        this.commandName = commandName;
    }

    /** @return the HTTP status code */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the command name, excluding parameters */
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean isOutcomeUnknown() {
        return statusCode < 400 || statusCode >= 500;
    }
}
