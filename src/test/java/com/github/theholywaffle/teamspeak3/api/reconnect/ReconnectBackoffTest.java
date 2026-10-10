package com.github.theholywaffle.teamspeak3.api.reconnect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReconnectBackoffTest {
	@Test
	void strategyContractDescribesRecoveryWithoutInspectingHandlerTypes() {
		assertFalse(ReconnectStrategy.disconnect().isReconnectEnabled());
		assertTrue(ReconnectStrategy.userControlled().isReconnectEnabled());
		assertTrue(ReconnectStrategy.constantBackoff(10).withMaxAttempts(2).isReconnectEnabled());
		assertTrue(ReconnectStrategy.linearBackoff(10, 1, 20).withMaxAttempts(2).isReconnectEnabled());
		assertTrue(ReconnectStrategy.exponentialBackoff(10, 2, 20).withMaxAttempts(2).isReconnectEnabled());
		assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.disconnect().withMaxAttempts(2));
		assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.userControlled().withMaxAttempts(2));
	}

	@Test
	void largeMultipliersAndAddendsCannotOverflowTheCap() {
		var exponential = new ReconnectingConnectionHandler(null, 1, Integer.MAX_VALUE, 0, Double.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, exponential.nextDelay(Integer.MAX_VALUE));
		var linear = new ReconnectingConnectionHandler(null, 10, 100, Integer.MAX_VALUE, 1);
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
