package com.github.theholywaffle.teamspeak3.api.reconnect;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectingConnectionHandler implements ConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReconnectingConnectionHandler.class);

    private final ConnectionHandler userConnectionHandler;
    private final int startTimeout;
    private final int timeoutCap;
    private final int addend;
    private final double multiplier;
    private final int maxAttempts;

    /** Creates a bounded reconnect handler; delays use equal jitter within the cap.
     * @param userConnectionHandler optional callback
     * @param startTimeout first delay upper bound in milliseconds
     * @param timeoutCap maximum delay upper bound in milliseconds
     * @param addend linear increase in milliseconds
     * @param multiplier exponential increase
     * @param maxAttempts maximum connection attempts per disconnect
     */
    public ReconnectingConnectionHandler(
            ConnectionHandler userConnectionHandler,
            int startTimeout,
            int timeoutCap,
            int addend,
            double multiplier,
            int maxAttempts) {
        if (startTimeout <= 0
                || timeoutCap < startTimeout
                || addend < 0
                || !Double.isFinite(multiplier)
                || multiplier < 1
                || maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "Reconnect requires positive attempts/delays, cap >= start, and finite multiplier >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.userConnectionHandler = userConnectionHandler;
        this.startTimeout = startTimeout;
        this.timeoutCap = timeoutCap;
        this.addend = addend;
        this.multiplier = multiplier;
    }

    @Override
    public void onConnect(TS3Api api) {
        if (userConnectionHandler != null) {
            userConnectionHandler.onConnect(api);
        }
    }

    @Override
    public void onDisconnect(TS3Query ts3Query) {
        // Announce disconnect and run user connection handler
        log.info(
                "Disconnected from TS3 server, reconnecting up to {} times, delay capped at {}ms",
                maxAttempts,
                timeoutCap);
        if (userConnectionHandler != null) {
            userConnectionHandler.onDisconnect(ts3Query);
        }

        int timeout = startTimeout;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (Thread.currentThread().isInterrupted() || ts3Query.getState() != TS3Query.State.DISCONNECTED) return;
            try {
                Thread.sleep(jitterDelay(timeout));
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            }
            if (ts3Query.getState() != TS3Query.State.DISCONNECTED) return;
            try {
                ts3Query.connect();
                return;
            } catch (TS3ConnectionFailedException e) {
                log.debug("Reconnect attempt {} failed", attempt + 1, e);
            } catch (IllegalStateException stateChanged) {
                if (ts3Query.getState() == TS3Query.State.CLOSED
                        || ts3Query.getState() == TS3Query.State.CLOSING
                        || ts3Query.getState() == TS3Query.State.CONNECTED) return;
                throw stateChanged;
            }
            timeout = nextDelay(timeout);
        }
        log.warn("Reconnect attempts exhausted ({})", maxAttempts);
        ts3Query.close();
    }

    int startTimeout() {
        return startTimeout;
    }

    int maxAttempts() {
        return maxAttempts;
    }

    int nextDelay(int delay) {
        long grown = Math.round(Math.ceil(delay * multiplier) + addend);
        return Math.clamp(grown, delay, timeoutCap);
    }

    static long jitterDelay(long upperBound) {
        return ThreadLocalRandom.current().nextLong(Math.max(1, (upperBound + 1) / 2), upperBound + 1);
    }
}
