package com.github.theholywaffle.teamspeak3.api.reconnect;

/**
 * What a query does after an unexpected connection loss. Configure it with
 * {@link com.github.theholywaffle.teamspeak3.TS3Config#setReconnectStrategy(ReconnectStrategy)}.
 * <p>
 * Built-in backoff strategies make up to ten connection attempts per disconnect, then close the query.
 * Each delay is chosen with equal jitter between half its upper bound and the upper bound, in milliseconds.
 * Commands whose outcome became unknown are never resent unless a
 * {@link CommandRetryPolicy} explicitly allows it; see {@link SessionConfiguration} for what is restored.
 * </p>
 */
public abstract class ReconnectStrategy {

    private static final int CONSTANT_BACKOFF = 10000;
    private static final int START_TIMEOUT = 1000;
    private static final int TIMEOUT_CAP = 60000;
    private static final int ADDEND = 2000;
    private static final double MULTIPLIER = 1.5;
    private static final int DEFAULT_ATTEMPTS = 10;

    private final boolean reconnectEnabled;

    private ReconnectStrategy(boolean reconnectEnabled) {
        this.reconnectEnabled = reconnectEnabled;
    }

    /**
     * @param userConnectionHandler the application's handler, or {@code null}
     * @return the handler the query calls on connect and disconnect
     */
    public abstract ConnectionHandler create(ConnectionHandler userConnectionHandler);

    /** @return whether this policy permits a new connection after transport loss */
    public final boolean isReconnectEnabled() {
        return reconnectEnabled;
    }

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

    /**
     * Leaves reconnecting to the application: {@link ConnectionHandler#onDisconnect} must call
     * {@code connect()} itself, otherwise the query closes when it returns.
     * @return a strategy that requires a connection handler
     */
    public static ReconnectStrategy userControlled() {
        return new UserControlled();
    }

    /** @return the default strategy: never reconnect, and close the query after a connection loss */
    public static ReconnectStrategy disconnect() {
        return new Disconnect();
    }

    /** @return constant delays of up to 10 seconds */
    public static ReconnectStrategy constantBackoff() {
        return constantBackoff(CONSTANT_BACKOFF);
    }

    /**
     * @param timeout the delay upper bound in milliseconds
     * @return constant delays
     */
    public static ReconnectStrategy constantBackoff(int timeout) {
        if (timeout <= 0) throw new IllegalArgumentException("Timeout must be greater than 0");
        return new Backoff(timeout, timeout, 0, 1.0, DEFAULT_ATTEMPTS);
    }

    /** @return delays starting at 1 second, growing by 2 seconds, capped at 60 seconds */
    public static ReconnectStrategy linearBackoff() {
        return linearBackoff(START_TIMEOUT, ADDEND, TIMEOUT_CAP);
    }

    /**
     * @param startTimeout the first delay upper bound in milliseconds
     * @param addend milliseconds added after each attempt
     * @return linearly growing delays, capped at 60 seconds
     */
    public static ReconnectStrategy linearBackoff(int startTimeout, int addend) {
        return linearBackoff(startTimeout, addend, TIMEOUT_CAP);
    }

    /**
     * @param startTimeout the first delay upper bound in milliseconds
     * @param addend milliseconds added after each attempt
     * @param timeoutCap the largest delay upper bound in milliseconds, at least {@code startTimeout}
     * @return linearly growing delays
     */
    public static ReconnectStrategy linearBackoff(int startTimeout, int addend, int timeoutCap) {
        if (addend <= 0) throw new IllegalArgumentException("Addend must be greater than 0");
        return backoff(startTimeout, timeoutCap, addend, 1.0);
    }

    /** @return delays starting at 1 second, multiplied by 1.5, capped at 60 seconds */
    public static ReconnectStrategy exponentialBackoff() {
        return exponentialBackoff(START_TIMEOUT, MULTIPLIER, TIMEOUT_CAP);
    }

    /**
     * @param startTimeout the first delay upper bound in milliseconds
     * @param multiplier the growth factor, greater than 1
     * @return exponentially growing delays, capped at 60 seconds
     */
    public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier) {
        return exponentialBackoff(startTimeout, multiplier, TIMEOUT_CAP);
    }

    /**
     * @param startTimeout the first delay upper bound in milliseconds
     * @param multiplier the growth factor, greater than 1
     * @param timeoutCap the largest delay upper bound in milliseconds, at least {@code startTimeout}
     * @return exponentially growing delays
     */
    public static ReconnectStrategy exponentialBackoff(int startTimeout, double multiplier, int timeoutCap) {
        if (!Double.isFinite(multiplier) || multiplier <= 1.0)
            throw new IllegalArgumentException("Multiplier must be greater than 1");
        return backoff(startTimeout, timeoutCap, 0, multiplier);
    }

    private static ReconnectStrategy backoff(int startTimeout, int timeoutCap, int addend, double multiplier) {
        if (startTimeout <= 0) throw new IllegalArgumentException("Starting timeout must be greater than 0");
        if (timeoutCap < startTimeout) throw new IllegalArgumentException("Cap must be >= starting timeout");
        return new Backoff(startTimeout, timeoutCap, addend, multiplier, DEFAULT_ATTEMPTS);
    }

    private static final class UserControlled extends ReconnectStrategy {
        private UserControlled() {
            super(true);
        }

        @Override
        public ConnectionHandler create(ConnectionHandler userConnectionHandler) {
            if (userConnectionHandler == null) {
                throw new IllegalArgumentException(
                        "userConnectionHandler cannot be null when using strategy UserControlled!");
            }
            return userConnectionHandler;
        }
    }

    private static final class Disconnect extends ReconnectStrategy {
        private Disconnect() {
            super(false);
        }

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
            return new ReconnectingConnectionHandler(
                    userConnectionHandler, startTimeout, timeoutCap, addend, multiplier, maxAttempts);
        }
    }
}
