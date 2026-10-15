package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/** Connecting or initializing the ServerQuery session failed. */
public class TS3ConnectionFailedException extends TS3TransportException {

    @Serial
    private static final long serialVersionUID = 6849777544299282019L;

    public TS3ConnectionFailedException(String msg) {
        super(msg);
    }

    public TS3ConnectionFailedException(Throwable c) {
        super("Could not connect to the TeamSpeak3 server", c);
    }

    public TS3ConnectionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
