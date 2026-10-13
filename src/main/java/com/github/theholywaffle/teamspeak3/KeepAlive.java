package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
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
