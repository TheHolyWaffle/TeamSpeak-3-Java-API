package com.github.theholywaffle.teamspeak3.api.exception;

import java.io.Serial;

/**
 * Base type of every failure reported by this library.
 * <p>
 * Subtypes identify the category: {@link TS3CommandFailedException} (server error response),
 * {@link TS3TransportException} (connection unavailable), {@link TS3TimeoutException} (deadline expired),
 * {@link TS3QueueFullException} (admission rejected), {@link TS3UnsupportedOperationException}
 * and {@link TS3FileTransferFailedException}. Use {@link #isOutcomeUnknown()} rather than a type
 * check to decide whether a failed command may still have executed.
 * </p>
 */
public class TS3Exception extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7167169981592989359L;

    public TS3Exception(String msg) {
        super(msg);
    }

    public TS3Exception(String msg, Throwable cause) {
        super(msg, cause);
    }

    /** Whether this failure represents the given ServerQuery error ID. */
    public boolean matchesQueryError(int errorId) {
        return false;
    }

    /**
     * Whether the server may have executed the failed ServerQuery command. Only retry such a command
     * when repeating it is safe. File transfer failures describe the separate file socket; see
     * {@link TS3FileTransferFailedException}.
     *
     * @return {@code true} if the command was written but its response never arrived
     */
    public boolean isOutcomeUnknown() {
        return false;
    }
}
