package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownOutcomeException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;

/**
 * Client for the HTTP(S) Web Query interface of a TeamSpeak 3 server (3.12 or later), exposing the same
 * domain APIs and models as {@link TS3Query}.
 * <p>
 * Web Query has no session: every command is an independent, API-key-authenticated HTTP request
 * addressed to the instance, a virtual server ID or a virtual server port. Asynchronous commands run
 * concurrently and may complete in any order. There is no connect step, keep-alive, reconnect or
 * notification stream. Operations the server does not offer over Web Query, such as server selection,
 * login, events and file commands, fail locally with
 * {@link TS3UnsupportedOperationException} without sending a request.
 * </p><p>
 * Instances are thread safe. Requests complete on virtual threads owned by this client; an open
 * client does not keep the JVM alive. Close it to abort requests and release its workers.
 * </p>
 */
public final class TS3WebQuery implements AutoCloseable {

    private final WebQueryConfig config;
    private final ExecutorService executor;
    private final HttpClient http;
    private final Semaphore admission;
    // Guarded by lock, keyed by request sequence number.
    private final Map<Long, CompletableFuture<?>> inFlight = new HashMap<>();
    private long nextRequest;
    private final Object lock = new Object();
    private final TS3ApiAsync instanceAsyncApi;
    private final TS3Api instanceApi;
    private volatile boolean closed;

    /** @param config immutable endpoint, API key and limits */
    public TS3WebQuery(WebQueryConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        admission = new Semaphore(config.getRequestCapacity());
        executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("[TeamSpeak-3-Java-API] WebQuery-", 0).factory());
        var builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(config.getConnectTimeout())
                .executor(executor);
        config.getSslContext().ifPresent(builder::sslContext);
        http = builder.build();
        instanceAsyncApi = api("");
        instanceApi = new TS3Api(instanceAsyncApi);
    }

    /** @return the synchronous API for instance commands such as {@code version} and {@code serverlist} */
    public TS3Api getApi() {
        return instanceApi;
    }

    /** @return the asynchronous API for instance commands */
    public TS3ApiAsync getAsyncApi() {
        return instanceAsyncApi;
    }

    /**
     * @param virtualServerId the virtual server every command of the returned API addresses
     * @return the synchronous API for that virtual server
     */
    public TS3Api getApi(int virtualServerId) {
        return new TS3Api(getAsyncApi(virtualServerId));
    }

    /**
     * @param virtualServerId the virtual server every command of the returned API addresses
     * @return the asynchronous API for that virtual server
     */
    public TS3ApiAsync getAsyncApi(int virtualServerId) {
        if (virtualServerId < 1) throw new IllegalArgumentException("Virtual server ID must be positive");
        return api(virtualServerId + "/");
    }

    /**
     * @param port the UDP voice port of the virtual server every command of the returned API addresses
     * @return the synchronous API for that virtual server
     */
    public TS3Api getApiByPort(int port) {
        return new TS3Api(getAsyncApiByPort(port));
    }

    /**
     * @param port the UDP voice port of the virtual server every command of the returned API addresses
     * @return the asynchronous API for that virtual server
     */
    public TS3ApiAsync getAsyncApiByPort(int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535");
        return api("byport/" + port + "/");
    }

    /** @return whether this client still accepts requests */
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Rejects new requests, aborts requests in flight and terminates the HTTP client and its workers
     * within the close budget. Aborted requests fail with an unknown outcome. Idempotent.
     */
    @Override
    public void close() {
        List<CompletableFuture<?>> aborted;
        synchronized (lock) {
            if (closed) return;
            closed = true;
            aborted = List.copyOf(inFlight.values());
        }
        for (CompletableFuture<?> exchange : aborted) exchange.cancel(true);
        Deadline deadline = new Deadline(config.getCloseTimeout());
        http.shutdownNow();
        try {
            var _ = http.awaitTermination(Duration.ofNanos(deadline.remaining() / 2));
            executor.shutdown();
            var _ = executor.awaitTermination(deadline.remaining(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    boolean resourcesTerminated() {
        return http.isTerminated() && executor.isTerminated();
    }

    private TS3ApiAsync api(String scope) {
        return new TS3ApiAsync(new WebQueryBackend(this, scope));
    }

    void send(String scope, Command command, CompletionStage<?> work) {
        CompletableFuture<DefaultArrayResponse> result = command.getFuture();
        var unsupported = WebQueryProtocol.unsupported(command.getName());
        if (unsupported.isPresent()) {
            result.completeExceptionally(new TS3UnsupportedOperationException(unsupported.get()));
            return;
        }
        // Reject before encoding the body: a full or closed client should stay cheap.
        if (closed) {
            result.completeExceptionally(new TS3QueryShutDownException("The Web Query client is closed"));
            return;
        }
        if (!admission.tryAcquire()) {
            result.completeExceptionally(new TS3QueueFullException());
            return;
        }
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(WebQueryProtocol.target(config.getBaseUri(), scope, command.getName()))
                    .timeout(config.getRequestTimeout())
                    .header(WebQueryProtocol.API_KEY_HEADER, config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(WebQueryProtocol.body(command), StandardCharsets.UTF_8))
                    .build();
        } catch (RuntimeException e) {
            admission.release();
            result.completeExceptionally(e);
            return;
        }
        CompletableFuture<HttpResponse<String>> exchange;
        long requestId;
        synchronized (lock) {
            if (closed) {
                admission.release();
                result.completeExceptionally(new TS3QueryShutDownException("The Web Query client is closed"));
                return;
            }
            exchange = http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            requestId = nextRequest++;
            inFlight.put(requestId, exchange);
        }
        var _ = exchange.whenComplete((response, failure) -> {
            synchronized (lock) {
                inFlight.remove(requestId);
            }
            try {
                if (failure == null) {
                    result.complete(
                            WebQueryProtocol.response(command.getName(), response.statusCode(), response.body()));
                } else {
                    result.completeExceptionally(failure(command.getName(), CommandFutures.unwrap(failure)));
                }
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            } finally {
                var _ = work.whenComplete((value, ignored) -> admission.release());
            }
        });
        var _ = result.whenComplete((value, failure) -> {
            if (result.isCancelled()) exchange.cancel(true);
        });
    }

    private TS3Exception failure(String commandName, Throwable failure) {
        try {
            throw failure;
        } catch (HttpConnectTimeoutException | ConnectException | SSLHandshakeException e) {
            return new TS3ConnectionFailedException(
                    "Could not connect to Web Query at " + config.getBaseUri() + " for " + commandName, e);
        } catch (HttpTimeoutException e) {
            var timeout = new TS3TimeoutException(TS3TimeoutException.Phase.RESPONSE, commandName);
            timeout.initCause(e);
            return timeout;
        } catch (Throwable other) {
            // Closing, cancellation and I/O errors can interrupt a request the server already received.
            var unknown = new TS3UnknownOutcomeException(commandName);
            unknown.initCause(other);
            return unknown;
        }
    }

    @Override
    public String toString() {
        return "TS3WebQuery[" + config.getBaseUri() + (closed ? ", closed]" : "]");
    }
}
