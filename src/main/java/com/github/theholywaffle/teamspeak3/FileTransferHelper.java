package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2016 Bert De Geyter, Roger Baumgartner
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.CRC32;

/** Owns only file sockets; caller streams are never flushed or closed. */
class FileTransferHelper implements AutoCloseable {
    private static final int BUFFER_SIZE = 16_384;
    private static final long SKIP_CHUNK = 1 << 20;
    private final AtomicInteger clientTransferId = new AtomicInteger();
    private final String defaultHost;
    private final FileTransferConfig config;
    private final ScheduledExecutorService deadlines;
    private final Set<Operation> operations = ConcurrentHashMap.newKeySet();
    private boolean closed;

    FileTransferHelper(String host, FileTransferConfig config, ScheduledExecutorService deadlines) {
        this.defaultHost = host;
        this.config = config;
        this.deadlines = deadlines;
    }

    synchronized Operation newOperation() {
        var operation = new Operation();
        if (closed) operation.abort();
        else operations.add(operation);
        return operation;
    }

    @Override
    public synchronized void close() {
        closed = true;
        for (var operation : operations) operation.abort();
    }

    final class Operation implements AutoCloseable {
        private Socket socket;
        private Thread worker;
        private boolean aborted;
        private boolean expired;
        private boolean ended;
        private ScheduledFuture<?> timer;
        private Runnable onAbort;

        synchronized void start(Runnable onAbort) throws IOException {
            check();
            worker = Thread.currentThread();
            this.onAbort = onAbort;
            timer = deadlines.schedule(
                    () -> {
                        synchronized (this) {
                            if (ended) return;
                            expired = true;
                            abort();
                        }
                    },
                    config.transferTimeout().toNanos(),
                    TimeUnit.NANOSECONDS);
        }

        synchronized void abort() {
            if (ended || aborted) return;
            aborted = true;
            closeSocket();
            if (worker != null) worker.interrupt();
            if (onAbort != null) onAbort.run();
        }

        synchronized IOException abortCause() {
            return expired
                    ? new SocketTimeoutException("File transfer deadline exceeded")
                    : new InterruptedIOException("File transfer aborted");
        }

        synchronized IOException failureCause(IOException failure) {
            return aborted ? abortCause() : failure;
        }

        synchronized void check() throws IOException {
            if (aborted || Thread.currentThread().isInterrupted()) throw abortCause();
        }

        Socket connect(FileTransferParameters params) throws IOException {
            check();
            int port = field(params::getFileServerPort);
            String key = field(params::getFileTransferKey);
            if (port < 1 || port > 65535 || key.isEmpty())
                throw new IOException("Invalid file transfer endpoint or key");
            var endpoint = config.endpointResolver()
                    .resolve(defaultHost, field(params::getFileServerHost).orElse(""), port);
            if (endpoint == null || endpoint.getPort() < 1) throw new IOException("Invalid resolved file endpoint");
            Socket connected;
            synchronized (this) {
                check();
                socket = new Socket();
                connected = socket;
            }
            connected.connect(endpoint, TS3Config.socketTimeout(config.connectTimeout()));
            connected.setSoTimeout(TS3Config.socketTimeout(config.readTimeout()));
            connected.getOutputStream().write(key.getBytes(StandardCharsets.UTF_8));
            connected.getOutputStream().flush();
            check();
            return connected;
        }

        @Override
        public synchronized void close() {
            ended = true;
            if (timer != null) timer.cancel(false);
            closeSocket();
            worker = null;
            onAbort = null;
            operations.remove(this);
        }

        private void closeSocket() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** Converts a malformed numeric server field into an I/O failure. */
    static <T> T field(Supplier<T> parse) throws IOException {
        try {
            return parse.get();
        } catch (TS3InvalidValueException malformed) {
            throw new IOException("Missing or malformed file transfer field", malformed);
        }
    }

    void downloadFile(OutputStream dataOut, FileTransferParameters params, long seekPosition, Operation operation)
            throws IOException {
        long size = field(params::getFileSize);
        if (size < 0 || seekPosition < 0 || seekPosition > size)
            throw new IOException("Invalid download length or seek position");
        try (Socket socket = operation.connect(params)) {
            InputStream in = socket.getInputStream();
            copy(
                    in,
                    dataOut,
                    size - seekPosition,
                    new byte[BUFFER_SIZE],
                    operation,
                    "Server response contained less data than specified or made no progress");
            expectEnd(in, operation);
        }
    }

    byte[] downloadBytes(FileTransferParameters params, Operation operation) throws IOException {
        long size = field(params::getFileSize);
        if (size < 0 || size > config.maxInMemoryBytes())
            throw new IOException("Invalid file size or in-memory limit exceeded");
        try (Socket socket = operation.connect(params)) {
            InputStream in = socket.getInputStream();
            byte[] data = readFully(in, size, config.maxInMemoryBytes(), operation);
            expectEnd(in, operation);
            return data;
        }
    }

    byte[] bufferFully(InputStream dataIn, long dataLength, Operation operation) throws IOException {
        return readFully(dataIn, dataLength, config.maxInMemoryBytes(), operation);
    }

    private static void expectEnd(InputStream in, Operation operation) throws IOException {
        operation.check();
        if (in.read() != -1) throw new IOException("Server response contained more data than specified");
        operation.check();
    }

    void uploadFile(
            InputStream dataIn, long dataLength, FileTransferParameters params, boolean resume, Operation operation)
            throws IOException {
        if (dataLength < 0) throw new IOException("Negative upload length");
        long position = field(params::getSeekPosition);
        if (position < 0 || position > dataLength || (!resume && position != 0))
            throw new IOException("Invalid server upload seek position");
        byte[] buffer = new byte[BUFFER_SIZE];
        // Resume callers provide the entire source; discard precisely the server-acknowledged prefix.
        skip(dataIn, position, buffer, operation);
        try (Socket socket = operation.connect(params)) {
            OutputStream out = socket.getOutputStream();
            copy(
                    dataIn,
                    out,
                    dataLength - position,
                    buffer,
                    operation,
                    "User stream did not contain enough data or made no progress");
            out.flush();
            operation.check();
        }
    }

    private static void copy(
            InputStream in, OutputStream out, long size, byte[] buffer, Operation operation, String shortReadMessage)
            throws IOException {
        while (size > 0) {
            operation.check();
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, size));
            if (read <= 0) throw new IOException(shortReadMessage);
            operation.check();
            out.write(buffer, 0, read);
            size -= read;
        }
    }

    private static void skip(InputStream in, long size, byte[] buffer, Operation operation) throws IOException {
        while (size > 0) {
            operation.check();
            long skipped = in.skip(Math.min(size, SKIP_CHUNK));
            if (skipped <= 0) {
                // skip cannot signal end of stream; a read can
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, size));
                if (read <= 0) throw new IOException("User stream did not contain resume prefix or made no progress");
                skipped = read;
            }
            size -= skipped;
        }
    }

    static long getIconId(byte[] data) {
        final CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    static byte[] readFully(InputStream dataIn, long dataLength, int limit, Operation operation) throws IOException {
        if (dataLength < 0 || dataLength > limit)
            throw new IOException("File exceeds in-memory limit or has negative length");
        byte[] data = new byte[(int) dataLength];
        int total = 0;
        while (total < data.length) {
            if (operation != null) operation.check();
            int read = dataIn.read(data, total, data.length - total);
            if (read <= 0) throw new IOException("Stream did not contain enough data or made no progress");
            total += read;
        }
        if (operation != null) operation.check();
        return data;
    }

    int getClientTransferId() {
        return clientTransferId.getAndUpdate(id -> (id + 1) & 0xFFFF);
    }
}
