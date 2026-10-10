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

import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** JDK future adapters used by the command API. */
public final class CommandFutures {
	private CommandFutures() { }

	/**
	 * Waits without abandoning the request on interruption; restores the interrupt flag.
	 * Translates JDK wrappers to the original TS3 exception, or wraps other failures.
	 * Cancellation remains a CancellationException.
	 */
	public static <T> T join(CompletableFuture<T> future) {
		try {
			return future.join();
		} catch (CompletionException failure) {
			Throwable cause = unwrap(failure);
			try { throw cause; }
			catch (TS3Exception | CancellationException exception) { throw exception; }
			catch (Throwable exception) { throw new TS3Exception("Asynchronous operation failed", exception); }
		}
	}

	/** Removes the standard JDK completion/execution wrappers. */
	public static Throwable unwrap(Throwable failure) {
		try { throw failure; }
		catch (CompletionException | ExecutionException wrapper) {
			return wrapper.getCause() == null ? wrapper : unwrap(wrapper.getCause());
		} catch (Throwable cause) { return cause; }
	}

	/** Links cancellation of an API result to its underlying requests. */
	public static <T> CompletableFuture<T> link(CompletableFuture<T> result, CompletableFuture<?>... requests) {
		var snapshot = requests.clone();
		for (var request : snapshot) Objects.requireNonNull(request);
		var linked = new RequestFuture<T>(() -> {
			for (var request : snapshot) request.cancel(false);
		});
		relay(result, linked);
		return linked;
	}

	private static <T> void relay(CompletableFuture<T> source, RequestFuture<T> target) {
		source.whenComplete((value, failure) -> {
			if (target.cancelling.get()) return;
			if (failure == null) target.complete(value);
			else target.completeExceptionally(unwrap(failure));
		});
	}

	/** JDK state/composition with request cancellation performed before application observers. */
	private static final class RequestFuture<T> extends CompletableFuture<T> {
		private final AtomicReference<Runnable> cancelRequest;
		private final AtomicBoolean cancelling = new AtomicBoolean();

		private RequestFuture(Runnable cancelRequest) {
			this.cancelRequest = new AtomicReference<>(cancelRequest);
			whenComplete((value, failure) -> this.cancelRequest.set(null));
		}

		@Override public boolean cancel(boolean mayInterruptIfRunning) {
			if (isDone()) return super.cancel(false);
			if (!cancelling.compareAndSet(false, true)) return isCancelled();
			try {
				var action = cancelRequest.getAndSet(null);
				if (action != null) action.run();
			} finally { super.cancel(false); }
			return isCancelled();
		}
	}

	/** Maps a result using JDK composition and links request cancellation. */
	public static <T, U> CompletableFuture<U> map(CompletableFuture<T> request, Function<? super T, ? extends U> fn) {
		return link(request.thenApply(fn), request);
	}

	/** Composes requests and links cancellation to both the initial and subsequent request. */
	public static <T, U> CompletableFuture<U> compose(CompletableFuture<T> request,
	                                               Function<? super T, CompletableFuture<U>> fn) {
		Objects.requireNonNull(fn);
		var cancelled = new AtomicBoolean();
		var child = new AtomicReference<CompletableFuture<U>>();
		var result = new RequestFuture<U>(() -> {
			cancelled.set(true);
			request.cancel(false);
			var next = child.get();
			if (next != null) next.cancel(false);
		});
		var composed = request.thenCompose(value -> {
			if (cancelled.get()) return CompletableFuture.<U>failedFuture(new CancellationException());
			var next = Objects.requireNonNull(fn.apply(value));
			child.set(next);
			if (cancelled.get()) next.cancel(false);
			return next;
		});
		relay(composed, result);
		return result;
	}

	/** Waits for every result, preserving input order. Fails as soon as any request fails. */
	public static <T> CompletableFuture<List<T>> all(Collection<CompletableFuture<T>> requests) {
		var snapshot = List.copyOf(requests);
		var result = CompletableFuture.allOf(snapshot.toArray(CompletableFuture[]::new))
			.thenApply(unused -> snapshot.stream().map(CompletableFuture::join).toList());
		for (var request : snapshot) request.whenComplete((value, failure) -> {
			if (failure != null) result.completeExceptionally(unwrap(failure));
		});
		return link(result, snapshot.toArray(CompletableFuture[]::new));
	}
}
