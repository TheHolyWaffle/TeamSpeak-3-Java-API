package com.github.theholywaffle.teamspeak3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class EventProbeTest {
    @Test @Timeout(1) void listenerOverflowFailsOnTheTestThreadWithoutWaitingForTimeout() {
        var probe = new EventProbe<String>(1);
        probe.publish("first"); probe.publish("overflow");
        assertEquals("Event probe overflow", assertThrows(AssertionError.class, probe::await).getMessage());
    }

    @Test @Timeout(1) void callbackFailuresWakeAndFailTheWaitingTest() {
        var probe = new EventProbe<String>(1);
        var failure = new IllegalStateException("callback failed");
        probe.capture(() -> { throw failure; });
        assertSame(failure, assertThrows(AssertionError.class, probe::await).getCause());
    }

    @Test @Timeout(1) void publishedEventsRetainTheirOrderAndIdentity() throws Exception {
        var probe = new EventProbe<Object>(2);
        var first = new Object(); var second = new Object();
        probe.publish(first); probe.publish(second);
        assertSame(first, probe.await()); assertSame(second, probe.await());
    }
}
