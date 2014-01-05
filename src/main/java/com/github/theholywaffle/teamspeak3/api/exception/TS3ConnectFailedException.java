package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3ConnectFailedException extends TS3Exception {
    public TS3ConnectFailedException(String message) {
        super(message);
    }

    public TS3ConnectFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
