package com.github.theholywaffle.teamspeak3.api.reconnect;

public abstract class ReconnectStrategy {

	private static final int CONSTANT_BACKOFF = 10000;
	private static final int START_TIMEOUT = 1000;
	private static final int TIMEOUT_CAP = 60000;
	private static final int ADDEND = 2000;
	private static final double MULTIPLIER = 1.5;

	private ReconnectStrategy() {}

	public abstract ConnectionHandler create(ConnectionHandler userConnectionHandler);

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
		return new Constant(timeout);
	}

	public static ReconnectStrategy linearBackoff() {
		return linearBackoff(START_TIMEOUT, ADDEND, TIMEOUT_CAP);
	}

	public static ReconnectStrategy linearBackoff(int startTimeout, int addend) {
		return linearBackoff(startTimeout, addend, TIMEOUT_CAP);
	}

	public static ReconnectStrategy linearBackoff(int startTimeout, int addend, int timeoutCap) {
		return new Linear(startTimeout, addend, timeoutCap);
	}

	public static ReconnectStrategy exponentialBackoff() {
		return exponentialBackoff(START_TIMEOUT, MULTIPLIER, TIMEOUT_CAP);
	}

	public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier) {
		return exponentialBackoff(startTimeout, multiplier, TIMEOUT_CAP);
	}

	public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier, int timeoutCap) {
		return new Exponential(startTimeout, multiplier, timeoutCap);
	}

	private static class UserControlled extends ReconnectStrategy {

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			String message = "userConnectionHandler cannot be null when using strategy UserControlled!";
			if (userConnectionHandler == null) throw new NullPointerException(message);
			return userConnectionHandler;
		}
	}

	private static class Disconnect extends ReconnectStrategy {

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new DisconnectingConnectionHandler(userConnectionHandler);
		}
	}

	private static class Constant extends ReconnectStrategy {

		private final int timeout;

		public Constant(int timeout) {
			if (timeout <= 0) throw new IllegalArgumentException("Timeout must be greater than 0");

			this.timeout = timeout;
		}

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new ReconnectingConnectionHandler(userConnectionHandler, timeout, timeout, 0, 1.0);
		}
	}

	private static class Linear extends ReconnectStrategy {

		private final int startTimeout;
		private final int addend;
		private final int timeoutCap;

		private Linear(int startTimeout, int addend, int timeoutCap) {
			if (startTimeout <= 0) throw new IllegalArgumentException("Starting timeout must be greater than 0");
			if (addend <= 0) throw new IllegalArgumentException("Addend must be greater than 0");

			this.startTimeout = startTimeout;
			this.addend = addend;
			this.timeoutCap = timeoutCap;
		}

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new ReconnectingConnectionHandler(userConnectionHandler, startTimeout, timeoutCap, addend, 1.0);
		}
	}

	private static class Exponential extends ReconnectStrategy {

		private final int startTimeout;
		private final double multiplier;
		private final int timeoutCap;

		private Exponential(int startTimeout, double multiplier, int timeoutCap) {
			if (startTimeout <= 0) throw new IllegalArgumentException("Starting timeout must be greater than 0");
			if (multiplier <= 1.0) throw new IllegalArgumentException("Multiplier must be greater than 1");

			this.startTimeout = startTimeout;
			this.multiplier = multiplier;
			this.timeoutCap = timeoutCap;
		}

		@Override
		public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
			return new ReconnectingConnectionHandler(userConnectionHandler, startTimeout, timeoutCap, 0, multiplier);
		}
	}
}
