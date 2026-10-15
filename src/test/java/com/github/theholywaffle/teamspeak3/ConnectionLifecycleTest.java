package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ConnectionLifecycleTest {
    private TS3Config config(int port) {
        return new TS3Config()
                .setHost("127.0.0.1")
                .setQueryPort(port)
                .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                .setConnectTimeout(Duration.ofMillis(300))
                .setHandshakeTimeout(Duration.ofMillis(300))
                .setQueueWaitTimeout(Duration.ofMillis(300))
                .setCommandResponseTimeout(Duration.ofMillis(300))
                .setCloseTimeout(Duration.ofMillis(500));
    }

    private void terminated(TS3Query query) throws Exception {
        long started = System.nanoTime();
        while (!query.resourcesTerminated() && System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1)) {
            Thread.sleep(5);
        }
        assertEquals(TS3Query.State.CLOSED, query.getState());
        assertFalse(query.isConnected());
        assertTrue(query.resourcesTerminated(), "Owned executors and I/O workers must terminate");
    }

    /** A completed command returns its permit only after its callbacks, which exit must not race. */
    @Test
    void exitWaitsForACompletingCommandsPermitBeforeQuit() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("whoami");
                    p.write("client_id=42\nerror id=0 msg=ok\n");
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                        .setCommandCapacity(1))) {
            query.connect();
            var whoami = query.getAsyncApi().whoAmI();
            var _ = whoami.thenRun(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            });
            whoami.join();
            query.exit();
            peer.await();
        }
    }

    @Test
    void closeBeforeConnectFailsPendingAndRejectsNewCommands() throws Exception {
        var query = new TS3Query(config(1));
        var pending = query.getAsyncApi().whoAmI();
        assertEquals(TS3Query.State.NEW, query.getState());
        assertTimeout(Duration.ofMillis(600), query::close);
        assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(pending, 1, TimeUnit.SECONDS));
        assertThrows(
                TS3QueryShutDownException.class,
                () -> FutureAssertions.read(query.getAsyncApi().whoAmI()));
        assertThrows(IllegalStateException.class, query::connect);
        query.close();
        query.exit();
        terminated(query);
    }

    @Test
    void exitBeforeConnectIsBounded() throws Exception {
        var query = new TS3Query(config(1));
        query.getAsyncApi().whoAmI();
        assertTimeout(Duration.ofMillis(600), query::exit);
        terminated(query);
    }

    @Test
    void doubleAndConcurrentCloseOfIdleSocket() throws Exception {
        var release = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(_ -> release.await(5, TimeUnit.SECONDS))) {
            var query = new TS3Query(config(peer.port()));
            query.connect();
            assertEquals(TS3Query.State.CONNECTED, query.getState());
            var other = CompletableFuture.runAsync(query::close);
            assertTimeout(Duration.ofMillis(600), query::close);
            other.get(1, TimeUnit.SECONDS);
            query.close();
            terminated(query);
            release.countDown();
        }
    }

    @Test
    void droppedServerFailsAllOutstandingRequests() throws Exception {
        var received = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(p -> {
            p.expect("whoami");
            received.countDown();
        })) {
            var query = new TS3Query(config(peer.port()));
            try {
                query.connect();
                var first = query.getAsyncApi().whoAmI();
                var second = query.getAsyncApi().whoAmI();
                assertTrue(received.await(1, TimeUnit.SECONDS));
                assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(first, 1, TimeUnit.SECONDS));
                assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(second, 1, TimeUnit.SECONDS));
                terminated(query);
            } finally {
                query.close();
            }
        }
    }

    @Test
    void responseDeadlineIgnoresNotificationsFragmentsAndNewCommands() throws Exception {
        var stop = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(p -> {
            p.expect("whoami");
            while (stop.getCount() != 0) {
                try {
                    p.write("clid=1\nnotifytextmessage\n");
                } catch (Exception _) {
                    return;
                }
                Thread.sleep(20);
            }
        })) {
            var query = new TS3Query(config(peer.port()));
            try {
                query.connect();
                var first = query.getAsyncApi().whoAmI();
                for (int i = 0; i < 10; i++) {
                    query.getAsyncApi().whoAmI();
                    Thread.sleep(20);
                }
                var failure = assertThrows(
                        TS3TimeoutException.class, () -> FutureAssertions.read(first, 700, TimeUnit.MILLISECONDS));
                assertEquals(TS3TimeoutException.Phase.RESPONSE, failure.getPhase());
                terminated(query);
            } finally {
                stop.countDown();
                query.close();
            }
        }
    }

    @Test
    void queueWaitDeadlineDoesNotConsumeResponseBudgetOrSendExpiredCommand() throws Exception {
        var release = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(p -> {
            p.expect("whoami");
            assertTrue(release.await(2, TimeUnit.SECONDS));
            p.write("clid=1\nerror id=0 msg=ok\n");
            p.finishQuit();
        })) {
            var query = new TS3Query(config(peer.port())
                    .setReconnectStrategy(ReconnectStrategy.exponentialBackoff())
                    .setQueueWaitTimeout(Duration.ofMillis(100))
                    .setCommandResponseTimeout(Duration.ofSeconds(2)));
            try {
                query.connect();
                var first = query.getAsyncApi().whoAmI();
                var waiting = query.getAsyncApi().whoAmI();
                var failure = assertThrows(
                        com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException.class,
                        () -> FutureAssertions.read(waiting, 500, TimeUnit.MILLISECONDS));
                assertFalse(failure.isOutcomeUnknown());
                assertFalse(first.isDone());
                release.countDown();
                assertNotNull(first.get(1, TimeUnit.SECONDS));
                query.exit();
                peer.await();
                terminated(query);
            } finally {
                release.countDown();
                query.close();
            }
        }
    }

    @Test
    void failedConnectAndPartialInitializationCleanUp() throws Exception {
        int port;
        try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = listener.getLocalPort();
        }
        var refused = new TS3Query(config(port));
        assertThrows(TS3ConnectionFailedException.class, refused::connect);
        terminated(refused);
        try (var peer = new FakeServerQuery(_ -> Thread.sleep(500))) {
            var query = new TS3Query(config(peer.port()).setConnectionHandler(new ConnectionHandler() {
                @Override
                public void onConnect(TS3Api api) {
                    throw new IllegalStateException("test initialization failure");
                }

                @Override
                public void onDisconnect(TS3Query query) {}
            }));
            assertThrows(TS3ConnectionFailedException.class, query::connect);
            terminated(query);
        }
    }

    @Test
    void handshakeDeadlineBoundsStalledLoginAndHandler() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
            p.expect("login client_login_name=test client_login_password=test");
            Thread.sleep(1000);
        })) {
            var query = new TS3Query(config(peer.port())
                    .setLoginCredentials("test", "test")
                    .setCommandResponseTimeout(Duration.ofSeconds(5)));
            assertTimeout(
                    Duration.ofMillis(900), () -> assertThrows(TS3ConnectionFailedException.class, query::connect));
            terminated(query);
        }
        var entered = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(_ -> Thread.sleep(1000))) {
            var query = new TS3Query(config(peer.port()).setConnectionHandler(new ConnectionHandler() {
                @Override
                public void onConnect(TS3Api api) {
                    entered.countDown();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                }

                @Override
                public void onDisconnect(TS3Query query) {}
            }));
            assertTimeout(
                    Duration.ofMillis(900), () -> assertThrows(TS3ConnectionFailedException.class, query::connect));
            assertEquals(0, entered.getCount());
            terminated(query);
        }
    }

    @Test
    void closeDuringInitializationDoesNotWaitForConnectMonitor() throws Exception {
        var entered = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(_ -> Thread.sleep(1000))) {
            var query = new TS3Query(config(peer.port())
                    .setHandshakeTimeout(Duration.ofSeconds(5))
                    .setConnectionHandler(new ConnectionHandler() {
                        @Override
                        public void onConnect(TS3Api api) {
                            entered.countDown();
                            api.whoAmI();
                        }

                        @Override
                        public void onDisconnect(TS3Query query) {}
                    }));
            var connect =
                    CompletableFuture.runAsync(() -> assertThrows(TS3ConnectionFailedException.class, query::connect));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertTimeout(Duration.ofMillis(600), query::close);
            connect.get(1, TimeUnit.SECONDS);
            terminated(query);
        }
    }

    @Test
    void blockedSocketWriteIsAbortedByDeadlineAndByClose() throws Exception {
        for (boolean timeout : new boolean[] {true, false}) {
            try (var peer = new FakeServerQuery(_ -> Thread.sleep(1500))) {
                var query = new TS3Query(config(peer.port())
                        .setCommandResponseTimeout(timeout ? Duration.ofMillis(700) : Duration.ofSeconds(5)));
                try {
                    query.connect();
                    // Larger than socket buffers: peer never reads commands.
                    var pending = query.getAsyncApi().setNickname("x".repeat(16 * 1024 * 1024));
                    long started = System.nanoTime();
                    boolean blocked = false;
                    while (!blocked && System.nanoTime() - started < TimeUnit.MILLISECONDS.toNanos(500)) {
                        blocked = Thread.getAllStackTraces().entrySet().stream()
                                .anyMatch(entry -> entry.getKey().getName().endsWith("StreamWriter")
                                        && java.util.Arrays.stream(entry.getValue())
                                                .anyMatch(frame -> frame.getClassName()
                                                                .equals("sun.nio.ch.NioSocketImpl")
                                                        && frame.getMethodName().equals("implWrite")));
                        if (!blocked) Thread.sleep(5);
                    }
                    assertTrue(blocked, "Test must observe the writer blocked in the socket write");
                    if (!timeout) assertTimeout(Duration.ofMillis(600), query::close);
                    Class<? extends TS3Exception> expected =
                            timeout ? TS3TimeoutException.class : TS3QueryShutDownException.class;
                    TS3Exception failure =
                            assertThrows(expected, () -> FutureAssertions.read(pending, 1, TimeUnit.SECONDS));
                    assertTrue(failure.isOutcomeUnknown());
                    terminated(query);
                } finally {
                    query.close();
                }
            }
        }
    }

    @Test
    void exitBoundsDrainingWhenServerNeverResponds() throws Exception {
        try (var peer = new FakeServerQuery(_ -> Thread.sleep(1000))) {
            var query = new TS3Query(config(peer.port()).setCommandResponseTimeout(Duration.ofSeconds(5)));
            query.connect();
            var pending = query.getAsyncApi().whoAmI();
            assertTimeout(Duration.ofMillis(600), query::exit);
            assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(pending, 1, TimeUnit.SECONDS));
            terminated(query);
        }
    }

    @Test
    void writerSurfacesIOExceptionAndFailsItsCommand() throws Exception {
        var config = config(1);
        try (var query = new TS3Query(config)) {
            var queue = CommandQueue.newConnectQueue(query);
            var con = new Connection(query, query.getConfig(), queue);
            var pending = queue.getAsyncApi().whoAmI();
            var writes = new CountDownLatch(1);
            var writer = Thread.ofPlatform()
                    .start(new StreamWriter(
                            con,
                            new OutputStream() {
                                @Override
                                public void write(int value) throws IOException {
                                    writes.countDown();
                                    throw new IOException("Injected write failure");
                                }
                            },
                            query.getConfig()));
            assertTrue(writes.await(1, TimeUnit.SECONDS));
            assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(pending, 1, TimeUnit.SECONDS));
            writer.join(1000);
            assertFalse(writer.isAlive());
            assertTrue(con.isStopped());
        }
    }

    @Test
    void missingSshCredentialsFailBeforeCreatingConnectionResources() {
        assertThrows(
                IllegalArgumentException.class, () -> new TS3Query(config(1).setProtocol(TS3Query.Protocol.SSH)));
    }

    @Test
    void sshHandshakeDeadlineAbortsSilentNegotiation() throws Exception {
        var connected = new CompletableFuture<Long>();
        var disconnected = new CountDownLatch(1);
        // Remain silent until the client closes, rather than ending negotiation via a peer timer.
        try (var peer = new FakeServerQuery(_ -> disconnected.await(5, TimeUnit.SECONDS))) {
            var settings = config(peer.port())
                    .setProtocol(TS3Query.Protocol.SSH)
                    .setConnectTimeout(Duration.ofMillis(50))
                    .setLoginCredentials("test", "test");
            // SSHJ/provider construction can take hundreds of milliseconds on a cold CI JVM.
            // Observe the actual TCP-connected boundary where the handshake budget starts.
            settings.setTransportFactory(new QueryTransportFactory() {
                @Override
                public boolean authenticates() {
                    return true;
                }

                @Override
                public QueryTransport create(QueryConfig config) {
                    var delegate = new SSHChannel(config);
                    // Deterministically exercise a cold/slow provider setup beyond the old whole-call bound.
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(failure);
                    }
                    return new QueryTransport() {
                        @Override
                        public void connect(Connected callback) throws java.io.IOException {
                            delegate.connect(() -> {
                                callback.transportConnected();
                                connected.complete(System.nanoTime());
                            });
                        }

                        @Override
                        public java.io.InputStream getInputStream() throws java.io.IOException {
                            return delegate.getInputStream();
                        }

                        @Override
                        public java.io.OutputStream getOutputStream() throws java.io.IOException {
                            return delegate.getOutputStream();
                        }

                        @Override
                        public void close() throws java.io.IOException {
                            delegate.close();
                            disconnected.countDown();
                        }

                        @Override
                        public void awaitTermination(Duration timeout) {
                            delegate.awaitTermination(timeout);
                        }

                        @Override
                        public boolean isTerminated() {
                            return delegate.isTerminated();
                        }
                    };
                }
            });
            try (var query = new TS3Query(settings)) {
                var attempt = CompletableFuture.supplyAsync(() -> {
                    assertThrows(TS3ConnectionFailedException.class, query::connect);
                    return System.nanoTime();
                });
                long started = connected.get(3, TimeUnit.SECONDS);
                long stopped = assertTimeout(Duration.ofMillis(900), () -> attempt.get(900, TimeUnit.MILLISECONDS));
                assertTrue(
                        stopped - started < TimeUnit.MILLISECONDS.toNanos(900),
                        "Handshake and cleanup must finish within the original 900 ms bound after TCP connection");
                assertTrue(
                        stopped - started >= TimeUnit.MILLISECONDS.toNanos(200),
                        "TCP deadline must not be reused for SSH negotiation");
                terminated(query);
                assertTrue(Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(thread -> thread.getName().startsWith("sshj-Reader-")
                                && thread.getName().contains(":" + peer.port() + "-")));
            }
        }
    }

    @Test
    void closeFromInitializationCallbackIsBounded() throws Exception {
        var reference = new java.util.concurrent.atomic.AtomicReference<TS3Query>();
        try (var peer = new FakeServerQuery(_ -> Thread.sleep(500))) {
            var query = new TS3Query(config(peer.port()).setConnectionHandler(new ConnectionHandler() {
                @Override
                public void onConnect(TS3Api api) {
                    reference.get().close();
                }

                @Override
                public void onDisconnect(TS3Query query) {}
            }));
            reference.set(query);
            assertTimeout(
                    Duration.ofMillis(900), () -> assertThrows(TS3ConnectionFailedException.class, query::connect));
            terminated(query);
        }
    }

    @Test
    void durationsArePositiveFiniteAndSnapshottedIndependently() {
        var config = config(1);
        assertThrows(IllegalArgumentException.class, () -> config.setConnectTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> config.setHandshakeTimeout(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> config.setQueueWaitTimeout(null));
        assertThrows(IllegalArgumentException.class, () -> config.setCloseTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
        config.setCommandResponseTimeout(Duration.ofMillis(123));
        assertEquals(Duration.ofMillis(123), config.getCommandResponseTimeout());
        assertEquals(Duration.ofMillis(300), config.getConnectTimeout());
        try (var query = new TS3Query(config)) {
            config.setCommandResponseTimeout(Duration.ofSeconds(1));
            assertNotEquals(
                    config.getCommandResponseTimeout(), query.getConfig().getCommandResponseTimeout());
        }
    }
}
