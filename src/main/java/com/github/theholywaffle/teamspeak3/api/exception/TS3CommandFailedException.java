package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3CommandFailedException extends TS3Exception {
    public TS3CommandFailedException(String message) {
        super(message);
    }

    public TS3CommandFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
