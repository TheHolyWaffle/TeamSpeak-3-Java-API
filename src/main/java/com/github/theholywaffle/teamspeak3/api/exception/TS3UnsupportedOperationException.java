package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/** The operation is not supported by this query's configuration or by the returned data. */
public class TS3UnsupportedOperationException extends TS3Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public TS3UnsupportedOperationException(String msg) {
        super(msg);
    }
}
