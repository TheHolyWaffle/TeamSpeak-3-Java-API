package com.github.theholywaffle.teamspeak3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Transfers listener failures to the test thread instead of losing them in event dispatch. */
final class EventProbe<T> {
    private final ArrayBlockingQueue<T> events;
    private final Semaphore available = new Semaphore(0);
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    EventProbe(int capacity) { events = new ArrayBlockingQueue<>(capacity); }

    void publish(T event) {
        if (!events.offer(event)) failure.compareAndSet(null, new AssertionError("Event probe overflow"));
        available.release();
    }

    void capture(Runnable callback) {
        try { callback.run(); }
        catch (Throwable error) { failure.compareAndSet(null, error); available.release(); }
    }

    T await() throws InterruptedException {
        assertTrue(available.tryAcquire(5, TimeUnit.SECONDS), "Expected notification before deadline");
        var error = failure.get();
        if (error != null) throw new AssertionError(error.getMessage(), error);
        var event = events.poll();
        assertNotNull(event, "Notification signal must own an event");
        return event;
    }
}
