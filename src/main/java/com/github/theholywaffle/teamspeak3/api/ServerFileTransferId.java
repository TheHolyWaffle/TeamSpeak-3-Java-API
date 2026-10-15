package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID the server assigns to a running file transfer ({@code serverftfid}), used to stop it.
 * It is not the {@link ClientFileTransferId} chosen by the transferring client, nor the key used to
 * connect to the file server.
 *
 * @param value the nonnegative ID
 */
public record ServerFileTransferId(int value) {

    public ServerFileTransferId {
        if (value < 0) throw new IllegalArgumentException("Server file transfer ID must not be negative");
    }

    public static ServerFileTransferId of(int value) {
        return new ServerFileTransferId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
