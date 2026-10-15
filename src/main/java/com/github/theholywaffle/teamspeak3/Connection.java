package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class Connection {
    private final TS3Query query;
    private final QueryConfig config;
    private final QueryTransport channel;
    private final AtomicReference<CommandQueue> queue;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicLong lastSent = new AtomicLong(System.nanoTime());
    private volatile Deadline initialization;
    private volatile Thread reader;
    private volatile Thread writer;
    private volatile Thread keepAlive;

    Connection(TS3Query query, QueryConfig config, CommandQueue initialQueue) {
        this.query = query;
        this.config = config;
        queue = new AtomicReference<>(initialQueue);
        channel = Objects.requireNonNull(config.getTransportFactory().create(config), "transport");
        initialization = new Deadline(config.getConnectTimeout());
    }

    void open() throws IOException {
        channel.connect(this::transportConnected);
        synchronized (this) {
            if (stopped.get()) throw new IOException("Connection closed during initialization");
            // Protocol I/O stays on platform threads, isolated from application work on virtual threads.
            reader = Thread.ofPlatform()
                    .name("[TeamSpeak-3-Java-API] StreamReader")
                    .daemon(true)
                    .unstarted(new StreamReader(this, channel.getInputStream(), query, config));
            writer = Thread.ofPlatform()
                    .name("[TeamSpeak-3-Java-API] StreamWriter")
                    .daemon(true)
                    .unstarted(new StreamWriter(this, channel.getOutputStream(), config));
            keepAlive =
                    Thread.ofVirtual().name("[TeamSpeak-3-Java-API] KeepAlive").unstarted(new KeepAlive(this));
            reader.start();
            writer.start();
            keepAlive.start();
        }
    }

    void transportConnected() throws IOException {
        if (stopped.get()) throw new IOException("Connection closed during connect");
        initialization = new Deadline(config.getHandshakeTimeout());
    }

    void initialized() {
        initialization = null;
    }

    boolean isStopped() {
        return stopped.get();
    }

    void checkDeadlines() {
        Deadline init = initialization;
        if (!stopped.get() && ((init != null && init.expired()) || queue.get().responseExpired())) {
            internalDisconnect();
        }
        CommandQueue currentQueue = queue.get();
        if (!currentQueue.isGlobal()) currentQueue.expireWaitingCommands();
    }

    void internalDisconnect() {
        if (!stop(new Deadline(config.getCloseTimeout()), true)) return;
        query.fireDisconnect(this);
    }

    void disconnect() {
        disconnect(new Deadline(config.getCloseTimeout()));
    }

    void disconnect(Deadline deadline) {
        if (!stop(deadline, false)) {
            queue.get().failRemainingCommands();
            awaitTermination(deadline);
        }
    }

    private boolean stop(Deadline deadline, boolean recover) {
        CommandQueue current;
        synchronized (this) {
            if (!stopped.compareAndSet(false, true)) return false;
            current = queue.get();
        }
        if (recover && current.isGlobal()) current.prepareReconnect();
        else current.failRemainingCommands();
        try {
            channel.close();
        } catch (IOException _) {
            // The connection is being torn down either way.
        }
        Thread[] threads;
        synchronized (this) {
            threads = new Thread[] {reader, writer, keepAlive};
        }
        for (Thread thread : threads) if (thread != null) thread.interrupt();
        awaitTermination(deadline);
        return true;
    }

    private void awaitTermination(Deadline deadline) {
        deadline.join(reader);
        deadline.join(writer);
        deadline.join(keepAlive);
        channel.awaitTermination(Duration.ofNanos(deadline.remaining()));
    }

    boolean threadsTerminated() {
        return channel.isTerminated()
                && (reader == null || !reader.isAlive())
                && (writer == null || !writer.isAlive())
                && (keepAlive == null || !keepAlive.isAlive());
    }

    CommandQueue getCommandQueue() {
        return queue.get();
    }

    void setCommandQueue(CommandQueue newQueue) {
        synchronized (this) {
            if (stopped.get()) throw new TS3ConnectionFailedException("Connection terminated during initialization");
            if (!queue.get().isEmpty()) throw new IllegalStateException("Old queue not empty");
            queue.set(newQueue);
            newQueue.resume();
        }
    }

    long getIdleTime() {
        return (System.nanoTime() - lastSent.get()) / 1_000_000L;
    }

    void resetIdleTime() {
        lastSent.set(System.nanoTime());
    }

    boolean isTimedOut() {
        return queue.get().responseExpired();
    }
}
