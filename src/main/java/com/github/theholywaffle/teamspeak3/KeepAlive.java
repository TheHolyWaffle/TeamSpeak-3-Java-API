package com.github.theholywaffle.teamspeak3;

import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sends a probe command whenever the connection has been idle for one interval. */
class KeepAlive implements Runnable {

    private static final long INTERVAL_MILLIS = 60_000;

    private static final Logger log = LoggerFactory.getLogger(KeepAlive.class);

    private final LongSupplier idleMillis;
    private final Supplier<CompletableFuture<?>> probe;
    private final Runnable onFailure;
    private final long intervalMillis;

    KeepAlive(Connection connection) {
        this(
                connection::getIdleTime,
                () -> connection.getCommandQueue().getAsyncApi().whoAmI(),
                connection::internalDisconnect,
                INTERVAL_MILLIS);
    }

    KeepAlive(LongSupplier idleMillis, Supplier<CompletableFuture<?>> probe, Runnable onFailure, long intervalMillis) {
        this.idleMillis = idleMillis;
        this.probe = probe;
        this.onFailure = onFailure;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long idleTime = idleMillis.getAsLong();
                if (idleTime >= intervalMillis) {
                    // Server errors still count as activity; only interruption or a local failure stops the loop.
                    probe.get().handle((value, failure) -> null).get();
                } else {
                    Thread.sleep(intervalMillis - idleTime);
                }
            }
        } catch (InterruptedException ignored) {
            // Thread stopped properly, ignore
        } catch (Exception e) {
            log.warn("KeepAlive thread has stopped!", e);
            onFailure.run();
        }
    }
}
