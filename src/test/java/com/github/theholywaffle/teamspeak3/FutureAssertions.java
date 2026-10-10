package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Keeps lifecycle assertions focused on the underlying library failure, with bounded waits. */
final class FutureAssertions {
	/** Value publication precedes callback cleanup; await capacity before expecting graceful quit. */
	static void awaitCommandAdmission(TS3Query query) throws InterruptedException {
		long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
		int capacity = query.getConfig().getCommandCapacity();
		while (query.commandAdmission(true).availablePermits() != capacity && System.nanoTime() < until) Thread.sleep(1);
		org.junit.jupiter.api.Assertions.assertEquals(capacity, query.commandAdmission(true).availablePermits());
	}

	static <T> T read(CompletableFuture<T> future) throws InterruptedException, TimeoutException {
		return read(future, 5, TimeUnit.SECONDS);
	}
	static <T> T read(CompletableFuture<T> future, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		try { return future.get(timeout, unit); }
		catch (ExecutionException failure) {
			Throwable cause = CommandFutures.unwrap(failure);
			if (cause instanceof TS3Exception exception) throw exception;
			throw new AssertionError("Unexpected future failure", cause);
		}
	}
}
