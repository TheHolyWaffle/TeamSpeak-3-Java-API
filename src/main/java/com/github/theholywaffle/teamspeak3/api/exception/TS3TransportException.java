package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/** The ServerQuery connection could not be established or is no longer usable. */
public class TS3TransportException extends TS3Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public TS3TransportException(String msg) {
        super(msg);
    }

    public TS3TransportException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
