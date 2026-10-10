package com.github.theholywaffle.teamspeak3;

/**
 * A local listener registration. Closing is idempotent and discards waiting events.
 * An already claimed callback may finish; close never waits for user code.
 * This does not change the server's notification registrations.
 */
@FunctionalInterface
public interface EventSubscription extends AutoCloseable {
	@Override void close();
}
