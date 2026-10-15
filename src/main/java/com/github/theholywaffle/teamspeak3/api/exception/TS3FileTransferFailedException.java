package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.IOException;
import java.io.Serial;

/**
 * The separate file-transfer socket or its local streams failed. An interrupted upload can leave
 * a partial file on the server.
 */
public class TS3FileTransferFailedException extends TS3Exception {

    @Serial
    private static final long serialVersionUID = 2819130214534186875L;

    public TS3FileTransferFailedException(String msg) {
        super(msg);
    }

    public TS3FileTransferFailedException(String msg, IOException cause) {
        super(msg, cause);
    }
}
