package com.github.theholywaffle.teamspeak3.api;

/**
 * The ID a client chooses for one of its file transfers ({@code clientftfid}). It is only unique
 * among that client's transfers and is not the {@link ServerFileTransferId} the server assigns.
 *
 * @param value the nonnegative ID
 */
public record ClientFileTransferId(int value) {

    public ClientFileTransferId {
        if (value < 0) throw new IllegalArgumentException("Client file transfer ID must not be negative");
    }

    public static ClientFileTransferId of(int value) {
        return new ClientFileTransferId(value);
    }

    /** @return the plain number, as used in ServerQuery commands */
    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
