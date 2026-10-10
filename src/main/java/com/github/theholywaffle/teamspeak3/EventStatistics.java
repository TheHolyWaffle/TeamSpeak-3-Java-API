package com.github.theholywaffle.teamspeak3;

/**
 * Monotonic query-local diagnostics; snapshots may include concurrent updates.
 * @param droppedEvents newest events discarded on listener/executor overflow
 * @param unknownNotifications unsupported notification lines ignored
 * @param malformedNotifications notification lines with no body or failing to parse
 * @param listenerFailures exceptions thrown by event callbacks (including lazy field access)
 * @param rejectedTasks event executor admission failures
 */
public record EventStatistics(long droppedEvents, long unknownNotifications,
		long malformedNotifications, long listenerFailures, long rejectedTasks) { }
