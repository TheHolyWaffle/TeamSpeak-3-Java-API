package com.github.theholywaffle.teamspeak3;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** A shared budget based only on monotonic elapsed time. */
final class Deadline {
	private final long started = System.nanoTime();
	private final long budget;
	Deadline(Duration timeout) { budget = timeout.toNanos(); }
	long remaining() { return Math.max(0L, budget - (System.nanoTime() - started)); }
	boolean expired() { return remaining() == 0; }
	void join(Thread thread) {
		if (thread == null || thread == Thread.currentThread()) return;
		long left = remaining();
		if (left == 0) return;
		try { TimeUnit.NANOSECONDS.timedJoin(thread, left); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}
}
