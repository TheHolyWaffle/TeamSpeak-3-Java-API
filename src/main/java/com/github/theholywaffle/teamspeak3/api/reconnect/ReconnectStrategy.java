package com.github.theholywaffle.teamspeak3.api.reconnect;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 - 2016 Bert De Geyter, Roger Baumgartner
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

public abstract class ReconnectStrategy {

	private static final int CONSTANT_BACKOFF = 10000;
	private static final int START_TIMEOUT = 1000;
	private static final int TIMEOUT_CAP = 60000;
	private static final int ADDEND = 2000;
	private static final double MULTIPLIER = 1.5;
	private static final int DEFAULT_ATTEMPTS = 10;

	private final boolean reconnectEnabled;

	private ReconnectStrategy(boolean reconnectEnabled) { this.reconnectEnabled = reconnectEnabled; }

	public abstract ConnectionHandler create(ConnectionHandler userConnectionHandler);

	/** @return whether this policy permits a new connection after transport loss */
	public final boolean isReconnectEnabled() { return reconnectEnabled; }

	/**
	 * Returns a copy using a finite attempt limit. Built-in backoff strategies default to ten attempts.
	 * All delays have a finite cap and equal jitter; interruption or query closure cancels retries.
	 * @param attempts maximum connection attempts per disconnect
	 * @return a bounded strategy
	 */
	public final ReconnectStrategy withMaxAttempts(int attempts) {
		if (attempts <= 0) throw new IllegalArgumentException("Attempts must be positive");
		return copyWithMaxAttempts(attempts);
	}

	ReconnectStrategy copyWithMaxAttempts(int attempts) {
		throw new IllegalArgumentException("Attempt limits require a built-in backoff strategy");
	}

	public static ReconnectStrategy userControlled() {
		return new UserControlled();
	}

	public static ReconnectStrategy disconnect() {
		return new Disconnect();
	}

	public static ReconnectStrategy constantBackoff() {
		return constantBackoff(CONSTANT_BACKOFF);
	}

	public static ReconnectStrategy constantBackoff(int timeout) {
		if (timeout <= 0) throw new IllegalArgumentException("Timeout must be greater than 0");
		return new Backoff(timeout, timeout, 0, 1.0, DEFAULT_ATTEMPTS);
	}

	public static ReconnectStrategy linearBackoff() {
		return linearBackoff(START_TIMEOUT, ADDEND, TIMEOUT_CAP);
	}

	public static ReconnectStrategy linearBackoff(int startTimeout, int addend) {
		return linearBackoff(startTimeout, addend, TIMEOUT_CAP);
	}

	public static ReconnectStrategy linearBackoff(int startTimeout, int addend, int timeoutCap) {
		if (addend <= 0) throw new IllegalArgumentException("Addend must be greater than 0");
		return backoff(startTimeout, timeoutCap, addend, 1.0);
	}

	public static ReconnectStrategy exponentialBackoff() {
		return exponentialBackoff(START_TIMEOUT, MULTIPLIER, TIMEOUT_CAP);
	}

	public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier) {
		return exponentialBackoff(startTimeout, multiplier, TIMEOUT_CAP);
	}

	public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier, int timeoutCap) {
		if (!Double.isFinite(multiplier) || multiplier <= 1.0) throw new IllegalArgumentException("Multiplier must be greater than 1");
		return backoff(startTimeout, timeoutCap, 0, multiplier);
	}

	private static ReconnectStrategy backoff(int startTimeout, int timeoutCap, int addend, double multiplier) {
		if (startTimeout <= 0) throw new IllegalArgumentException("Starting timeout must be greater than 0");
		if (timeoutCap < startTimeout) throw new IllegalArgumentException("Cap must be >= starting timeout");
		return new Backoff(startTimeout, timeoutCap, addend, multiplier, DEFAULT_ATTEMPTS);
	}

	private static final class UserControlled extends ReconnectStrategy {
		private UserControlled() { super(true); }

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			if (userConnectionHandler == null) {
				throw new IllegalArgumentException("userConnectionHandler cannot be null when using strategy UserControlled!");
			}
			return userConnectionHandler;
		}
	}

	private static final class Disconnect extends ReconnectStrategy {
		private Disconnect() { super(false); }

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new DisconnectingConnectionHandler(userConnectionHandler);
		}
	}

	/** Constant, linear and exponential delays: {@code min(cap, ceil(delay * multiplier) + addend)}. */
	private static final class Backoff extends ReconnectStrategy {

		private final int startTimeout;
		private final int timeoutCap;
		private final int addend;
		private final double multiplier;
		private final int maxAttempts;

		private Backoff(int startTimeout, int timeoutCap, int addend, double multiplier, int maxAttempts) {
			super(true);
			this.startTimeout = startTimeout;
			this.timeoutCap = timeoutCap;
			this.addend = addend;
			this.multiplier = multiplier;
			this.maxAttempts = maxAttempts;
		}

		@Override
		ReconnectStrategy copyWithMaxAttempts(int attempts) {
			return new Backoff(startTimeout, timeoutCap, addend, multiplier, attempts);
		}

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new ReconnectingConnectionHandler(userConnectionHandler, startTimeout, timeoutCap, addend, multiplier, maxAttempts);
		}
	}
}
