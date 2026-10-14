package com.github.theholywaffle.teamspeak3.api.reconnect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReconnectBackoffTest {
    @Test
    void strategyContractDescribesRecoveryWithoutInspectingHandlerTypes() {
        assertFalse(ReconnectStrategy.disconnect().isReconnectEnabled());
        assertTrue(ReconnectStrategy.userControlled().isReconnectEnabled());
        assertTrue(ReconnectStrategy.constantBackoff(10).withMaxAttempts(2).isReconnectEnabled());
        assertTrue(ReconnectStrategy.linearBackoff(10, 1, 20).withMaxAttempts(2).isReconnectEnabled());
        assertTrue(ReconnectStrategy.exponentialBackoff(10, 2, 20)
                .withMaxAttempts(2)
                .isReconnectEnabled());
        assertThrows(
                IllegalArgumentException.class,
                () -> ReconnectStrategy.disconnect().withMaxAttempts(2));
        assertThrows(
                IllegalArgumentException.class,
                () -> ReconnectStrategy.userControlled().withMaxAttempts(2));
    }

    @Test
    void userControlledReconnectUsesTheApplicationHandlerAndRequiresOne() {
        var application = new DisconnectingConnectionHandler(null);
        assertSame(application, ReconnectStrategy.userControlled().create(application));
        assertThrows(
                IllegalArgumentException.class,
                () -> ReconnectStrategy.userControlled().create(null));
    }

    @Test
    void builtInStrategiesKeepTheirDelayProgressionAndAttemptLimit() {
        assertEquals(List.of(100L, 100L, 100L), delays(ReconnectStrategy.constantBackoff(100), 3));
        assertEquals(List.of(10L, 15L, 20L, 25L, 30L, 30L), delays(ReconnectStrategy.linearBackoff(10, 5, 30), 6));
        assertEquals(List.of(10L, 20L, 40L, 50L), delays(ReconnectStrategy.exponentialBackoff(10, 2, 50), 4));
        assertEquals(List.of(1000L, 1500L, 2250L), delays(ReconnectStrategy.exponentialBackoff(), 3));
        assertEquals(List.of(1000L, 3000L, 5000L), delays(ReconnectStrategy.linearBackoff(), 3));
        assertEquals(10, handler(ReconnectStrategy.constantBackoff()).maxAttempts());
        assertEquals(
                3, handler(ReconnectStrategy.linearBackoff().withMaxAttempts(3)).maxAttempts());
        assertEquals(
                List.of(10L, 20L),
                delays(ReconnectStrategy.exponentialBackoff(10, 2, 50).withMaxAttempts(3), 2));
        assertEquals(
                "Timeout must be greater than 0",
                assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.constantBackoff(0))
                        .getMessage());
        assertEquals(
                "Addend must be greater than 0",
                assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.linearBackoff(10, 0))
                        .getMessage());
        assertEquals(
                "Multiplier must be greater than 1",
                assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.exponentialBackoff(10, 1))
                        .getMessage());
    }

    private static ReconnectingConnectionHandler handler(ReconnectStrategy strategy) {
        return (ReconnectingConnectionHandler) strategy.create(null);
    }

    private static List<Long> delays(ReconnectStrategy strategy, int count) {
        var handler = handler(strategy);
        var delays = new ArrayList<Long>();
        for (long delay = handler.startTimeout(); delays.size() < count; delay = handler.nextDelay(delay))
            delays.add(delay);
        return delays;
    }

    @Test
    void largeMultipliersAndAddendsCannotOverflowTheCap() {
        var exponential = new ReconnectingConnectionHandler(null, 1, Integer.MAX_VALUE, 0, Double.MAX_VALUE, 10);
        assertEquals(Integer.MAX_VALUE, exponential.nextDelay(Integer.MAX_VALUE));
        var linear = new ReconnectingConnectionHandler(null, 10, 100, Integer.MAX_VALUE, 1, 10);
        assertEquals(100, linear.nextDelay(10));
    }

    @Test
    void jitterStaysPositiveWithinEqualJitterBounds() {
        for (long upper : new long[] {1, 2, 10, 60_000, Integer.MAX_VALUE}) {
            for (int i = 0; i < 100; i++) {
                long jittered = ReconnectingConnectionHandler.jitterDelay(upper);
                assertTrue(jittered >= Math.max(1, (upper + 1) / 2));
                assertTrue(jittered <= upper);
            }
        }
    }
}
