package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class KeepAliveTest {

    @Test
    void probesOnlyAfterAFullIdleIntervalAndStopsOnInterrupt() throws Exception {
        var idle = new AtomicLong(0);
        var probes = new CountDownLatch(2);
        var failures = new AtomicInteger();
        var thread = Thread.ofPlatform()
                .start(new KeepAlive(
                        idle::get,
                        () -> {
                            probes.countDown();
                            return CompletableFuture.completedFuture(null);
                        },
                        failures::incrementAndGet,
                        20));
        Thread.sleep(60);
        assertEquals(2, probes.getCount(), "An active connection needs no probe");
        idle.set(20);
        assertTrue(probes.await(5, TimeUnit.SECONDS));
        thread.interrupt();
        thread.join(5000);
        assertFalse(thread.isAlive());
        assertEquals(0, failures.get(), "Interruption is a regular stop");
    }

    @Test
    void serverErrorsKeepTheLoopRunning() throws Exception {
        var probes = new CountDownLatch(3);
        var failures = new AtomicInteger();
        var thread = Thread.ofPlatform()
                .start(new KeepAlive(
                        () -> 10,
                        () -> {
                            probes.countDown();
                            return CompletableFuture.failedFuture(new IllegalStateException("error id=1024"));
                        },
                        failures::incrementAndGet,
                        10));
        assertTrue(probes.await(5, TimeUnit.SECONDS));
        thread.interrupt();
        thread.join(5000);
        assertEquals(0, failures.get());
    }

    @Test
    void localFailureStopsTheLoopAndDisconnectsOnce() throws Exception {
        var failures = new AtomicInteger();
        var thread = Thread.ofPlatform()
                .start(new KeepAlive(
                        () -> 10,
                        () -> {
                            throw new TS3QueryShutDownException("queue closed");
                        },
                        failures::incrementAndGet,
                        10));
        thread.join(5000);
        assertFalse(thread.isAlive());
        assertEquals(1, failures.get());
    }
}
