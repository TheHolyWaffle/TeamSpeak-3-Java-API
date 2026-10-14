package com.github.theholywaffle.teamspeak3;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

/** A fresh ordered UTF-8 ServerQuery byte stream for one connection.
 * Implementations own only transport I/O: framing, commands and events are shared by TS3Query.
 * close must abort concurrent connect/read/write promptly, including partially initialized resources.
 */
public interface QueryTransport extends Closeable {
    /** Callback marking the transition from connect to handshake deadline. */
    @FunctionalInterface
    interface Connected {
        /** @throws IOException when connection initialization was cancelled */
        void transportConnected() throws IOException;
    }

    /** @param connection call once after connect, before handshake/authentication
     * @throws IOException if transport initialization fails */
    void connect(Connected connection) throws IOException;

    /** @param timeout remaining shutdown budget for owned workers */
    default void awaitTermination(Duration timeout) {}

    /** @return whether every owned worker has stopped */
    default boolean isTerminated() {
        return true;
    }

    /** @return ordered query input @throws IOException if unavailable */
    InputStream getInputStream() throws IOException;

    /** @return ordered query output @throws IOException if unavailable */
    OutputStream getOutputStream() throws IOException;
}
