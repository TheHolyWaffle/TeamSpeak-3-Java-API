package com.github.theholywaffle.teamspeak3.api;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CommandFuture<V> implements Future<V> {

	private final AtomicReference<V> value = new AtomicReference<>();
	private final AtomicReference<Throwable> throwable = new AtomicReference<>();
	private final AtomicBoolean done = new AtomicBoolean(false);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private final AtomicReference<SuccessListener<V>> successListener = new AtomicReference<>();
	private final AtomicReference<FailureListener> failureListener = new AtomicReference<>();

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (done.get()) return false; // We're already done

		done.set(true);
		cancelled.set(true);
		synchronized (this) {
			notifyAll();
		}
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled.get();
	}

	@Override
	public boolean isDone() {
		return done.get();
	}

	@Override
	public V get() throws CancellationException, TS3CommandFailedException {
		boolean interrupted = false;
		while (!done.get()) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}

		if (interrupted) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		checkForFailure();
		return value.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws CancellationException, TS3CommandFailedException, TimeoutException {
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);
		boolean interrupted = false;
		while (!done.get() && System.currentTimeMillis() < end) {
			try {
				synchronized (this) {
					wait(end - System.currentTimeMillis());
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}

		if (interrupted) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		if (!done.get()) throw new TimeoutException();
		checkForFailure();
		return value.get();
	}

	private void checkForFailure() throws CancellationException, TS3CommandFailedException {
		if (cancelled.get()) {
			throw new CancellationException();
		}

		Throwable t = throwable.get();
		if (t != null) {
			throw new TS3CommandFailedException(t);
		}
	}

	public void fail(Throwable throwable) {
		if (done.get()) return; // Ignore

		done.set(true);
		this.throwable.set(throwable);
		synchronized (this) {
			notifyAll();
		}

		final FailureListener listener = failureListener.get();
		if (listener != null) {
			try {
				listener.handleFailure(throwable);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public boolean isFailed() {
		return throwable.get() != null;
	}

	public void set(V value) {
		if (done.get()) return; // Ignore

		done.set(true);
		this.value.set(value);
		synchronized (this) {
			notifyAll();
		}

		final SuccessListener<V> listener = successListener.get();
		if (listener != null) {
			try {
				listener.handleSuccess(value);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public CommandFuture<V> onSuccess(SuccessListener<V> successListener) {
		if (this.successListener.get() != null) {
			throw new IllegalStateException("Listener already set");
		}
		this.successListener.set(successListener);
		return this;
	}

	public CommandFuture<V> forwardSuccess(final CommandFuture<V> otherFuture) {
		return onSuccess(new SuccessListener<V>() {
			@Override
			public void handleSuccess(V result) {
				otherFuture.set(result);
			}
		});
	}

	public CommandFuture<V> onFailure(FailureListener failureListener) {
		if (this.failureListener.get() != null) {
			throw new IllegalStateException("Listener already set");
		}
		this.failureListener.set(failureListener);
		return this;
	}

	public CommandFuture<V> forwardFailure(final CommandFuture<?> otherFuture) {
		return onFailure(new FailureListener() {
			@Override
			public void handleFailure(Throwable throwable) {
				otherFuture.fail(throwable);
			}
		});
	}

	public CommandFuture<V> forwardResult(final CommandFuture<V> otherFuture) {
		return forwardSuccess(otherFuture).forwardFailure(otherFuture);
	}

	@SafeVarargs
	public static <F> CommandFuture<Collection<F>> awaitAll(CommandFuture<F>... futures) {
		return awaitAll(Arrays.asList(futures));
	}

	public static <F> CommandFuture<Collection<F>> awaitAll(final Collection<CommandFuture<F>> futures) {
		if (futures.isEmpty()) throw new IllegalArgumentException("Requires at least 1 future");

		final CommandFuture<Collection<F>> combined = new CommandFuture<>();
		final Map<Integer, F> results = Collections.synchronizedMap(new TreeMap<Integer, F>());
		final AtomicInteger successCounter = new AtomicInteger(futures.size());

		Iterator<CommandFuture<F>> iterator = futures.iterator();
		for (int i = 0; iterator.hasNext(); ++i) {
			final int index = i;
			final CommandFuture<F> future = iterator.next();

			future.forwardFailure(combined).onSuccess(new SuccessListener<F>() {
				@Override
				public void handleSuccess(F result) {
					results.put(index, result);

					if (successCounter.decrementAndGet() == 0) {
						combined.set(results.values());
					}
				}
			});
		}

		return combined;
	}

	@SafeVarargs
	public static <F> CommandFuture<F> awaitAny(CommandFuture<F>... futures) {
		return awaitAny(Arrays.asList(futures));
	}

	public static <F> CommandFuture<F> awaitAny(final Collection<CommandFuture<F>> futures) {
		if (futures.isEmpty()) throw new IllegalArgumentException("Requires at least 1 future");

		final CommandFuture<F> any = new CommandFuture<>();
		final AtomicInteger failureCounter = new AtomicInteger(futures.size());

		for (CommandFuture<F> future : futures) {
			future.forwardSuccess(any).onFailure(new FailureListener() {
				@Override
				public void handleFailure(Throwable throwable) {
					if (failureCounter.decrementAndGet() == 0) {
						any.fail(throwable);
					}
				}
			});
		}

		return any;
	}

	public interface SuccessListener<V> {

		void handleSuccess(V result);
	}

	public interface FailureListener {

		void handleFailure(Throwable throwable);
	}
}
