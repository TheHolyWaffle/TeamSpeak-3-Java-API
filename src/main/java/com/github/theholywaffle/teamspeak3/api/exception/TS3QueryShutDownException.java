package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/**
 * The query was closed or disconnected before the command completed. Thrown directly, the command was never
 * written; the {@link TS3UnknownOutcomeException} subtype means it may have reached the server.
 */
public class TS3QueryShutDownException extends TS3TransportException {

    @Serial
    private static final long serialVersionUID = -6727279731231409306L;

    public TS3QueryShutDownException() {
        this("The query was shut down or disconnected.");
    }

    public TS3QueryShutDownException(String msg) {
        super(msg);
    }
}
