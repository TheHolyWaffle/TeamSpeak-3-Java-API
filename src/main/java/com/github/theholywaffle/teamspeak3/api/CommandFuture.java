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

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents the result of an asynchronous execution of a query command.
 * <p>
 * Basically, this class is a container for a server response which will
 * arrive at some time in the future. It also accounts for the possibility
 * that a command might fail and that a future might be cancelled by a user.
 * </p>
 * A {@code CommandFuture} can therefore have 4 different states:
 * <ul>
 * <li><b>Waiting</b> - No response from the server has arrived yet</li>
 * <li><b>Cancelled</b> - A user cancelled this future before a response from the server could arrive</li>
 * <li><b>Failed</b> - The server received the command but responded with an error message</li>
 * <li><b>Succeeded</b> - The server successfully processed the command and sent back a result</li>
 * </ul>
 * You can check the state of the future using the methods {@link #isDone()},
 * {@link #isSuccessful()}, {@link #hasFailed()} and {@link #isCancelled()}.
 * <p>
 * A {@code CommandFuture}'s value can be retrieved by calling {@link #get()}
 * or {@link #get(long, TimeUnit)}, which block the current thread until the
 * server response arrives. The method with a timeout should be preferred
 * as there's no guarantee that a proper response (or an error message)
 * will ever arrive, e.g. in case of a permanent disconnect.
 * There are also variations of these methods which ignore thread interrupts,
 * {@link #getUninterruptibly()} and {@link #getUninterruptibly(long, TimeUnit)}.
 * </p><p>
 * Note that <b>these methods</b> all wait for the response to arrive and thereby
 * <b>revert to synchronous</b> execution. If you want to handle the server response
 * asynchronously, you need to register success and failure listeners.
 * These listeners will be called in a separate thread once a response arrives.
 * </p><p>
 * Each {@code CommandFuture} can only ever have one {@link SuccessListener} and
 * one {@link FailureListener} registered. All {@link TS3ApiAsync} methods are
 * guaranteed to return a {@code CommandFuture} with no listeners registered.
 * </p><p>
 * To set the value of a {@code CommandFuture}, the {@link #set(Object)} method is used;
 * to notify it of a failure, {@link #fail(TS3Exception)} is used. You usually
 * shouldn't call these methods yourself, however. That's the job of the API.
 * </p><p>
 * {@code CommandFuture}s are thread-safe. All state-changing methods are synchronized.
 * </p>
 *
 * @param <V>
 * 		the type of the value
 *
 * @see TS3ApiAsync
 */
public class CommandFuture<V> implements Future<V> {

	private static final Logger log = LoggerFactory.getLogger(CommandFuture.class);

	private enum FutureState {
		WAITING,
		CANCELLED,
		FAILED,
		SUCCEEDED
	}

	/**
	 * Just a plain object used for its monitor to synchronize access to the
	 * critical sections of this future and to signal state changes to any
	 * threads waiting in {@link #get()} and {@link #getUninterruptibly()} methods.
	 */
	private final Object monitor = new Object();

	private volatile FutureState state = FutureState.WAITING;
	private volatile V value = null;
	private volatile TS3Exception exception = null;
	private volatile SuccessListener<? super V> successListener = null;
	private volatile FailureListener failureListener = null;

	/**
	 * Waits indefinitely until the command completes.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, this method will throw an {@code InterruptedException}
	 * and the thread's interrupt flag will be cleared.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @throws InterruptedException
	 * 		if the method is interrupted by calling {@link Thread#interrupt()}.
	 * 		The interrupt flag will be cleared
	 */
	public void await() throws InterruptedException {
		while (state == FutureState.WAITING) {
			synchronized (monitor) {
				monitor.wait();
			}
		}
	}

	/**
	 * Waits for at most the given time until the command completes.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, this method will throw an {@code InterruptedException}
	 * and the thread's interrupt flag will be cleared.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @param timeout
	 * 		the maximum amount of the given time unit to wait
	 * @param unit
	 * 		the time unit of the timeout argument
	 *
	 * @throws InterruptedException
	 * 		if the method is interrupted by calling {@link Thread#interrupt()}.
	 * 		The interrupt flag will be cleared
	 * @throws TimeoutException
	 * 		if the given time elapsed without the command completing
	 */
	public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);
		while (state == FutureState.WAITING && System.currentTimeMillis() < end) {
			synchronized (monitor) {
				monitor.wait(end - System.currentTimeMillis());
			}
		}

		if (state == FutureState.WAITING) throw new TimeoutException();
	}

	/**
	 * Waits indefinitely until the command completes.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, the interrupt is simply ignored and no
	 * {@link InterruptedException} is thrown.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 */
	public void awaitUninterruptibly() {
		boolean interrupted = false;
		while (state == FutureState.WAITING) {
			try {
				synchronized (monitor) {
					monitor.wait();
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}

		if (interrupted) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Waits for at most the given time until the command completes.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, the interrupt is simply ignored and no
	 * {@link InterruptedException} is thrown.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @param timeout
	 * 		the maximum amount of the given time unit to wait
	 * @param unit
	 * 		the time unit of the timeout argument
	 *
	 * @throws TimeoutException
	 * 		if the given time elapsed without the command completing
	 */
	public void awaitUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);
		boolean interrupted = false;
		while (state == FutureState.WAITING && System.currentTimeMillis() < end) {
			try {
				synchronized (monitor) {
					monitor.wait(end - System.currentTimeMillis());
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}

		if (interrupted) {
			// Restore the interrupt for the caller
			Thread.currentThread().interrupt();
		}

		if (state == FutureState.WAITING) throw new TimeoutException();
	}

	/**
	 * Waits indefinitely until the command completes
	 * and returns the result of the command.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, this method will throw an {@code InterruptedException}
	 * and the thread's interrupt flag will be cleared.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @return the server response to the command
	 *
	 * @throws InterruptedException
	 * 		if the method is interrupted by calling {@link Thread#interrupt()}.
	 * 		The interrupt flag will be cleared
	 * @throws CancellationException
	 * 		if the {@code CommandFuture} was cancelled before the command completed
	 * @throws TS3Exception
	 * 		if the command fails
	 */
	@Override
	public V get() throws InterruptedException {
		await();

		checkForFailure();
		return value;
	}

	/**
	 * Waits for at most the given time until the command completes
	 * and returns the result of the command.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, this method will throw an {@code InterruptedException}
	 * and the thread's interrupt flag will be cleared.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @param timeout
	 * 		the maximum amount of the given time unit to wait
	 * @param unit
	 * 		the time unit of the timeout argument
	 *
	 * @return the server response to the command
	 *
	 * @throws InterruptedException
	 * 		if the method is interrupted by calling {@link Thread#interrupt()}.
	 * 		The interrupt flag will be cleared
	 * @throws TimeoutException
	 * 		if the given time elapsed without the command completing
	 * @throws CancellationException
	 * 		if the {@code CommandFuture} was cancelled before the command completed
	 * @throws TS3Exception
	 * 		if the command fails
	 */
	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		await(timeout, unit);

		checkForFailure();
		return value;
	}

	/**
	 * Waits indefinitely until the command completes
	 * and returns the result of the command.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, the interrupt is simply ignored and no
	 * {@link InterruptedException} is thrown.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @return the server response to the command
	 *
	 * @throws CancellationException
	 * 		if the {@code CommandFuture} was cancelled before the command completed
	 * @throws TS3Exception
	 * 		if the command fails
	 */
	public V getUninterruptibly() {
		awaitUninterruptibly();

		checkForFailure();
		return value;
	}

	/**
	 * Waits for at most the given time until the command completes
	 * and returns the result of the command.
	 * <p>
	 * If the thread is interrupted while waiting for the command
	 * to complete, the interrupt is simply ignored and no
	 * {@link InterruptedException} is thrown.
	 * </p><p><i>
	 * Please note that this method is blocking and thus negates
	 * the advantage of the asynchronous nature of this class.
	 * Consider using {@link #onSuccess(SuccessListener)} and
	 * {@link #onFailure(FailureListener)} instead.
	 * </i></p>
	 *
	 * @param timeout
	 * 		the maximum amount of the given time unit to wait
	 * @param unit
	 * 		the time unit of the timeout argument
	 *
	 * @return the server response to the command
	 *
	 * @throws TimeoutException
	 * 		if the given time elapsed without the command completing
	 * @throws CancellationException
	 * 		if the {@code CommandFuture} was cancelled before the command completed
	 * @throws TS3Exception
	 * 		if the command fails
	 */
	public V getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
		awaitUninterruptibly(timeout, unit);

		checkForFailure();
		return value;
	}

	/**
	 * Throws an exception if the future was either cancelled or the command failed.
	 *
	 * @throws CancellationException
	 * 		if the future was cancelled
	 * @throws TS3Exception
	 * 		if the command failed
	 */
	private void checkForFailure() {
		if (state == FutureState.CANCELLED) {
			throw new CancellationException();
		} else if (state == FutureState.FAILED) {
			// Make the stack trace of the exception point to this method and not
			// SocketReader#run -> TS3ApiAsync#hasFailed, which wouldn't be helpful
			exception.fillInStackTrace();
			throw exception;
		}
	}

	@Override
	public boolean isDone() {
		return state != FutureState.WAITING;
	}

	/**
	 * Returns {@code true} if this command completed successfully,
	 * i.e. the future wasn't cancelled and the command completed without throwing an exception.
	 *
	 * @return {@code true} if the command completed successfully
	 */
	public boolean isSuccessful() {
		return state == FutureState.SUCCEEDED;
	}

	@Override
	public boolean isCancelled() {
		return state == FutureState.CANCELLED;
	}

	/**
	 * Returns {@code true} if the command failed and threw a {@link TS3Exception}.
	 *
	 * @return {@code true} if the command failed
	 */
	public boolean hasFailed() {
		return state == FutureState.FAILED;
	}

	/**
	 * Sets the value of this future. This will mark the future as successful.
	 * <p>
	 * Furthermore, this will run the {@link SuccessListener}, if one is registered.
	 * All exceptions thrown from the body of the {@code SuccessListener} are caught
	 * so no exceptions can leak into user code.
	 * </p><p>
	 * Note that a future's value can only be set once. Subsequent calls to
	 * this method will be ignored.
	 * </p>
	 *
	 * @param value
	 * 		the value to set this future to
	 *
	 * @return {@code true} if the command was marked as successful
	 */
	public boolean set(V value) {
		synchronized (monitor) {
			if (isDone()) return false; // Ignore

			this.state = FutureState.SUCCEEDED;
			this.value = value;
			monitor.notifyAll();
		}

		if (successListener != null) {
			try {
				successListener.handleSuccess(value);
			} catch (Exception e) {
				// Whatever happens, we do not want a user error to leak into our logic
				log.error("User SuccessListener threw an exception", e);
			}
		}
		return true;
	}

	/**
	 * Notifies this future that the command has failed.
	 * <p>
	 * Furthermore, this will run the {@link FailureListener}, if one is registered.
	 * All exceptions thrown from the body of the {@code FailureListener} are caught
	 * so no exceptions can leak into user code.
	 * </p><p>
	 * Note that a future can only fail once. Subsequent calls to this method will be ignored.
	 * </p>
	 *
	 * @param exception
	 * 		the exception that occurred while executing this command
	 *
	 * @return {@code true} if the command was marked as failed
	 */
	public boolean fail(TS3Exception exception) {
		synchronized (monitor) {
			if (isDone()) return false; // Ignore

			this.state = FutureState.FAILED;
			this.exception = exception;
			monitor.notifyAll();
		}

		if (failureListener != null) {
			try {
				failureListener.handleFailure(exception);
			} catch (Exception e) {
				// Whatever happens, we do not want a user error to leak into our logic
				log.error("User FailureListener threw an exception", e);
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Cancelling a {@code CommandFuture} will <b>not</b> actually cancel the
	 * execution of the command which was sent to the TeamSpeak server.
	 * </p><p>
	 * It will, however, prevent the {@link SuccessListener} and the
	 * {@link FailureListener} from firing, provided a response from the
	 * server has not yet arrived.
	 * </p>
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		synchronized (monitor) {
			if (isDone()) return false; // Ignore

			this.state = FutureState.CANCELLED;
			monitor.notifyAll();
		}

		return true;
	}

	/**
	 * Sets a {@link SuccessListener} which will be notified when this future
	 * succeeded and a value has been set.
	 * <p>
	 * If this future has already succeeded, this method will immediately call
	 * the listener method, which will be executed synchronously.
	 * </p>
	 *
	 * @param listener
	 * 		the listener to notify of a success
	 *
	 * @return this object for chaining
	 */
	public CommandFuture<V> onSuccess(SuccessListener<? super V> listener) {
		synchronized (monitor) {
			if (successListener != null) {
				throw new IllegalStateException("Listener already set");
			}
			successListener = listener;
		}

		if (state == FutureState.SUCCEEDED) {
			listener.handleSuccess(value);
		}

		return this;
	}

	/**
	 * Sets a {@link FailureListener} which will be notified when this future
	 * fails because of a error returned by the TeamSpeak server.
	 * <p>
	 * If this future has already failed, this method will immediately call
	 * the listener method, which will be executed synchronously.
	 * </p>
	 *
	 * @param listener
	 * 		the listener to notify of a failure
	 *
	 * @return this object for chaining
	 */
	public CommandFuture<V> onFailure(FailureListener listener) {
		synchronized (monitor) {
			if (failureListener != null) {
				throw new IllegalStateException("Listener already set");
			}
			failureListener = listener;
		}

		if (state == FutureState.FAILED) {
			listener.handleFailure(exception);
		}

		return this;
	}

	/**
	 * Forwards a success to another future by calling {@link #set(Object)} on
	 * that future with the value this future was set to.
	 * <p>
	 * This will register a {@link SuccessListener}, meaning that you will not
	 * be able to register another {@code SuccessListener}.
	 * </p>
	 *
	 * @param otherFuture
	 * 		the future to forward a success to
	 *
	 * @return this object for chaining
	 */
	public CommandFuture<V> forwardSuccess(final CommandFuture<? super V> otherFuture) {
		return onSuccess(new SuccessListener<V>() {
			@Override
			public void handleSuccess(V result) {
				otherFuture.set(result);
			}
		});
	}

	/**
	 * Forwards a failure to another future by calling {@link #fail(TS3Exception)}
	 * on that future with the error that caused this future to fail.
	 * <p>
	 * This will register a {@link FailureListener}, meaning that you will not
	 * be able to register another {@code FailureListener}.
	 * </p>
	 *
	 * @param otherFuture
	 * 		the future to forward a failure to
	 *
	 * @return this object for chaining
	 */
	public CommandFuture<V> forwardFailure(final CommandFuture<?> otherFuture) {
		return onFailure(new FailureListener() {
			@Override
			public void handleFailure(TS3Exception exception) {
				otherFuture.fail(exception);
			}
		});
	}

	/**
	 * Forwards both a success as well as a failure to another {@code CommandFuture}.
	 * This method just calls both {@link #forwardSuccess(CommandFuture)} and
	 * {@link #forwardFailure(CommandFuture)}.
	 * <p>
	 * This will set both a {@link SuccessListener} as well as a {@link FailureListener},
	 * so no other listeners can be registered.
	 * </p>
	 *
	 * @param otherFuture
	 * 		the future which should be notified about
	 */
	public void forwardResult(final CommandFuture<V> otherFuture) {
		forwardSuccess(otherFuture).forwardFailure(otherFuture);
	}

	/**
	 * Returns a new {@code CommandFuture} that already has a value set.
	 *
	 * @param value
	 * 		the default value for the new {@code CommandFuture}
	 * @param <V>
	 * 		the dynamic type of the value, will usually be inferred
	 *
	 * @return a new {@code CommandFuture} with a default value
	 */
	public static <V> CommandFuture<V> immediate(V value) {
		final CommandFuture<V> future = new CommandFuture<>();
		future.set(value);
		return future;
	}

	/**
	 * Combines multiple {@code CommandFuture}s into a single future, which will
	 * succeed if all futures succeed and fail as soon as one future fails.
	 *
	 * @param futures
	 * 		the futures to combine
	 * @param <F>
	 * 		the common return type of the futures
	 *
	 * @return a future which succeeds if all supplied futures succeed
	 */
	@SafeVarargs
	public static <F> CommandFuture<List<F>> ofAll(CommandFuture<F>... futures) {
		return ofAll(Arrays.asList(futures));
	}

	/**
	 * Combines a collection of {@code CommandFuture}s into a single future, which will
	 * succeed if all futures succeed and fail as soon as one future fails.
	 *
	 * @param futures
	 * 		the futures to combine
	 * @param <F>
	 * 		the common return type of the futures
	 *
	 * @return a future which succeeds if all supplied futures succeed
	 */
	public static <F> CommandFuture<List<F>> ofAll(final Collection<CommandFuture<F>> futures) {
		if (futures.isEmpty()) throw new IllegalArgumentException("Requires at least 1 future");

		@SuppressWarnings("unchecked") final F[] results = (F[]) new Object[futures.size()];
		final AtomicInteger successCounter = new AtomicInteger(futures.size());
		final CommandFuture<List<F>> combined = new CommandFuture<>();

		final Iterator<CommandFuture<F>> iterator = futures.iterator();
		for (int i = 0; iterator.hasNext(); ++i) {
			final int index = i;
			final CommandFuture<F> future = iterator.next();

			future.forwardFailure(combined).onSuccess(new SuccessListener<F>() {
				@Override
				public void handleSuccess(F result) {
					results[index] = result;

					if (successCounter.decrementAndGet() == 0) {
						combined.set(Arrays.asList(results));
					}
				}
			});
		}

		return combined;
	}

	/**
	 * Combines multiple {@code CommandFuture}s into a single future, which will
	 * succeed if any of the futures succeeds and fail if all of the futures fail.
	 *
	 * @param futures
	 * 		the futures to combine
	 * @param <F>
	 * 		the common return type of the futures
	 *
	 * @return a future which succeeds if one of the supplied futures succeeds
	 */
	@SafeVarargs
	public static <F> CommandFuture<F> ofAny(CommandFuture<F>... futures) {
		return ofAny(Arrays.asList(futures));
	}

	/**
	 * Combines a collection of {@code CommandFuture}s into a single future, which will
	 * succeed as soon as one of the futures succeeds and fail if all futures fail.
	 *
	 * @param futures
	 * 		the futures to combine
	 * @param <F>
	 * 		the common return type of the futures
	 *
	 * @return a future which succeeds if one of the supplied futures succeeds
	 */
	public static <F> CommandFuture<F> ofAny(final Collection<CommandFuture<F>> futures) {
		if (futures.isEmpty()) throw new IllegalArgumentException("Requires at least 1 future");

		final CommandFuture<F> any = new CommandFuture<>();
		final AtomicInteger failureCounter = new AtomicInteger(futures.size());

		for (CommandFuture<F> future : futures) {
			future.forwardSuccess(any).onFailure(new FailureListener() {
				@Override
				public void handleFailure(TS3Exception exception) {
					if (failureCounter.decrementAndGet() == 0) {
						any.fail(exception);
					}
				}
			});
		}

		return any;
	}

	/**
	 * A listener which will be notified if the {@link CommandFuture} succeeded.
	 * In that case, {@link #handleSuccess(Object)} will be called with the value
	 * the future has been set to.
	 * <p>
	 * A {@code CommandFuture}'s {@code SuccessListener} can be set by calling
	 * {@link #onSuccess(SuccessListener)}.
	 * </p>
	 *
	 * @param <V>
	 * 		the type of the value
	 */
	public interface SuccessListener<V> {

		/**
		 * The method to be executed when the command succeeds.
		 *
		 * @param result
		 * 		the result of the command
		 */
		void handleSuccess(V result);
	}

	/**
	 * A listener which will be notified if the {@link CommandFuture} failed.
	 * In that case, {@link #handleFailure(TS3Exception)} will be called with
	 * the exception that occurred while executing this command.
	 * <p>
	 * A {@code CommandFuture}'s {@code FailureListener} can be set by calling
	 * {@link #onFailure(FailureListener)}.
	 * </p>
	 */
	public interface FailureListener {

		/**
		 * The method to be executed when the command failed.
		 *
		 * @param exception
		 * 		the exception that occurred while executing this command
		 */
		void handleFailure(TS3Exception exception);
	}
}
