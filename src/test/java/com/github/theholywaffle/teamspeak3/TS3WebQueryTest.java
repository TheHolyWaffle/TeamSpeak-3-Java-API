package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.UploadMode;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.exception.TS3AuthenticationFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3HttpException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownOutcomeException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class TS3WebQueryTest {

    private static final String KEY = "BAByFoiEXZfnSJyE6dbXFiW_nn_SdwkclpKNz9j";
    private static final String OK = "{\"body\":[{\"version\":\"3.13.8\",\"build\":\"1\",\"platform\":\"Linux\"}],"
            + "\"status\":{\"code\":0,\"message\":\"ok\"}}";

    record Request(String method, String path, String rawQuery, String apiKey, String contentType, String body) {}

    record Reply(int status, String body) {}

    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private final ExecutorService serverThreads = Executors.newVirtualThreadPerTaskExecutor();
    private volatile Function<Request, Reply> responder = request -> new Reply(200, OK);
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(serverThreads);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        serverThreads.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var request = new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawPath(),
                    exchange.getRequestURI().getRawQuery(),
                    exchange.getRequestHeaders().getFirst("x-api-key"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requests.add(request);
            Reply reply = responder.apply(request);
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), body.length == 0 ? -1 : body.length);
            if (body.length > 0) exchange.getResponseBody().write(body);
        }
    }

    private WebQueryConfig.Builder config() {
        return WebQueryConfig.builder(
                        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/prefix"), KEY)
                .setPlaintextHttpAllowed(true)
                .setRequestTimeout(Duration.ofSeconds(2))
                .setCloseTimeout(Duration.ofSeconds(2));
    }

    @Test
    void postsAuthenticatedJsonToTheScopedCommandPath() throws Exception {
        try (var query = new TS3WebQuery(config().build())) {
            assertEquals("3.13.8", query.getApi().instance().version().version());
            assertEquals(new Request("POST", "/prefix/version", null, KEY, "application/json", "{}"), requests.take());

            responder = request -> new Reply(200, "{\"body\":[{\"cid\":\"5\"}],\"status\":{\"code\":0}}");
            assertEquals(
                    5, query.getApi(3).channels().create("a b|c", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
            Request create = requests.take();
            assertEquals("/prefix/3/channelcreate", create.path());
            assertEquals("{\"channel_name\":\"a b|c\",\"channel_flag_permanent\":\"1\"}", create.body());
            assertNull(create.rawQuery(), "the API key never appears in the URL");

            responder = request -> new Reply(200, "{\"status\":{\"code\":0}}");
            query.getApiByPort(9987).channels().delete(5);
            assertEquals("/prefix/byport/9987/channeldelete", requests.take().path());
            CommandFutures.join(query.getAsyncApi(2).channels().delete(5));
            assertEquals("/prefix/2/channeldelete", requests.take().path());
            CommandFutures.join(query.getAsyncApiByPort(1).channels().delete(5));
            assertEquals("/prefix/byport/1/channeldelete", requests.take().path());
        }
    }

    @Test
    void sendsAServerMessageToTheAddressedVirtualServer() throws Exception {
        responder = request -> new Reply(200, "{\"status\":{\"code\":0}}");
        try (var query = new TS3WebQuery(config().build())) {
            query.getApi().messages().sendServer(2, "hi");
            Request request = requests.take();
            assertEquals("/prefix/2/sendtextmessage", request.path());
            assertEquals("{\"targetmode\":\"3\",\"target\":\"0\",\"msg\":\"hi\"}", request.body());
            assertThrows(
                    IllegalArgumentException.class,
                    () -> query.getApi().messages().sendServer(0, "hi"));
        }
    }

    @Test
    void rejectsInvalidScopes() {
        try (var query = new TS3WebQuery(config().build())) {
            assertSame(query.getApi(), query.getApi());
            assertSame(query.getAsyncApi(), query.getAsyncApi());
            assertThrows(IllegalArgumentException.class, () -> query.getApi(0));
            assertThrows(IllegalArgumentException.class, () -> query.getAsyncApi(-1));
            assertThrows(IllegalArgumentException.class, () -> query.getApiByPort(0));
            assertThrows(IllegalArgumentException.class, () -> query.getAsyncApiByPort(65536));
        }
    }

    @Test
    void sessionEventAndFileOperationsFailLocallyWithoutARequest() {
        try (var query = new TS3WebQuery(config().build())) {
            TS3Api api = query.getApi(1);
            TS3ApiAsync async = query.getAsyncApi(1);
            List<Runnable> operations = List.of(
                    () -> api.selectVirtualServerById(1),
                    () -> api.selectVirtualServerByPort(9987),
                    () -> api.login("serveradmin", "secret"),
                    api::logout,
                    () -> api.events().registerAll(),
                    () -> api.events().unregisterAll(),
                    () -> api.events().subscribe(new TS3EventAdapter() {}),
                    () -> async.events().subscribe(new TS3EventAdapter() {}),
                    () -> api.events().register(TS3EventType.SERVER),
                    () -> api.files().list(FileLocation.of(1, "/")),
                    () -> api.files().transfers(),
                    () -> api.files().createDirectory(FileLocation.of(1, "/d")),
                    () -> api.files().upload(FileLocation.of(1, "/f"), new byte[] {1}, UploadMode.CREATE),
                    () -> api.files().downloadBytes(FileLocation.of(1, "/f")),
                    () -> api.files().download(FileLocation.of(1, "/f"), new ByteArrayOutputStream()),
                    () -> api.files().uploadIcon(new byte[] {1, 2, 3}),
                    () -> api.files().uploadIcon(new java.io.ByteArrayInputStream(new byte[] {1}), 1),
                    () -> api.files().icons(),
                    () -> api.executeRawCommand("help channellist"),
                    () -> api.executeRawCommand("ftgetfilelist cid=0 cpw= path=\\/"));
            for (Runnable operation : operations) {
                var error = assertThrows(TS3UnsupportedOperationException.class, operation::run);
                assertTrue(error.getMessage().contains("Web Query"), error.getMessage());
            }
            assertTrue(requests.isEmpty(), requests.toString());
        }
    }

    @Test
    void translatesServerAndAuthenticationErrors() {
        try (var query = new TS3WebQuery(config().build())) {
            responder = request -> new Reply(200, "{\"status\":{\"code\":768,\"message\":\"invalid channelID\"}}");
            var failed = assertThrows(
                    TS3CommandFailedException.class,
                    () -> query.getApi(1).channels().info(9));
            assertEquals(768, failed.getErrorId());
            assertTrue(failed.matchesQueryError(768));

            responder = request -> new Reply(
                    401,
                    "{\"status\":{\"code\":5120,\"extra_message\":\"command not in api key scope\","
                            + "\"message\":\"out of scope\"}}");
            var denied = assertThrows(
                    TS3AuthenticationFailedException.class,
                    () -> query.getApi(1).channels().delete(1));
            assertEquals(5120, denied.getErrorId());
            assertEquals("channeldelete", denied.getCommandName());
            assertFalse(denied.isOutcomeUnknown());

            responder = request -> new Reply(502, "<html>proxy error</html>");
            var proxy = assertThrows(
                    TS3HttpException.class, () -> query.getApi().instance().version());
            assertEquals(502, proxy.getStatusCode());
            assertTrue(proxy.isOutcomeUnknown());
        }
    }

    @Test
    void expiredRequestsHaveAnUnknownOutcome() throws Exception {
        var release = new CountDownLatch(1);
        responder = request -> {
            awaitQuietly(release);
            return new Reply(200, OK);
        };
        try (var query = new TS3WebQuery(
                config().setRequestTimeout(Duration.ofMillis(200)).build())) {
            var error = assertThrows(
                    TS3TimeoutException.class, () -> query.getApi().instance().version());
            assertEquals(TS3TimeoutException.Phase.RESPONSE, error.getPhase());
            assertEquals("version", error.getCommandName());
            assertTrue(error.isOutcomeUnknown());
        } finally {
            release.countDown();
        }
    }

    @Test
    void unreachableServersFailToConnect() throws Exception {
        int port;
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        var config = WebQueryConfig.builder(URI.create("http://127.0.0.1:" + port), KEY)
                .setPlaintextHttpAllowed(true)
                .setConnectTimeout(Duration.ofSeconds(1))
                .build();
        try (var query = new TS3WebQuery(config)) {
            var error = assertThrows(
                    TS3ConnectionFailedException.class,
                    () -> query.getApi().instance().version());
            assertFalse(error.isOutcomeUnknown());
            assertTrue(error.getMessage().contains("version"));
        }
    }

    @Test
    void boundsRequestsInFlightAndReleasesCapacity() throws Exception {
        var arrived = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        responder = request -> {
            arrived.countDown();
            awaitQuietly(release);
            return new Reply(200, OK);
        };
        try (var query = new TS3WebQuery(config().setRequestCapacity(1).build())) {
            var first = query.getAsyncApi().instance().version();
            assertTrue(arrived.await(5, TimeUnit.SECONDS));
            assertThrows(
                    TS3QueueFullException.class, () -> query.getApi().instance().version());
            release.countDown();
            assertEquals("3.13.8", CommandFutures.join(first).version());
            responder = request -> new Reply(200, OK);
            assertEquals("3.13.8", awaitCapacity(query).version());
        }
    }

    @Test
    void cancellingARequestReleasesItsCapacity() throws Exception {
        var arrived = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        responder = request -> {
            arrived.countDown();
            awaitQuietly(release);
            return new Reply(200, OK);
        };
        try (var query = new TS3WebQuery(config().setRequestCapacity(1).build())) {
            var pending = query.getAsyncApi().instance().version();
            assertTrue(arrived.await(5, TimeUnit.SECONDS));
            assertTrue(pending.cancel(true));
            responder = request -> new Reply(200, OK);
            assertEquals("3.13.8", awaitCapacity(query).version());
        } finally {
            release.countDown();
        }
    }

    @Test
    void closeAbortsRequestsRejectsNewOnesAndTerminatesWorkers() throws Exception {
        var arrived = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        responder = request -> {
            arrived.countDown();
            awaitQuietly(release);
            return new Reply(200, OK);
        };
        var query = new TS3WebQuery(config().build());
        try {
            assertTrue(query.isOpen());
            CompletableFuture<?> pending = query.getAsyncApi().instance().version();
            assertTrue(arrived.await(5, TimeUnit.SECONDS));
            query.close();
            assertFalse(query.isOpen());
            var aborted = assertThrows(TS3UnknownOutcomeException.class, () -> CommandFutures.join(pending));
            assertTrue(aborted.isOutcomeUnknown());
            var rejected = assertThrows(
                    TS3QueryShutDownException.class,
                    () -> query.getApi().instance().version());
            assertFalse(rejected.isOutcomeUnknown());
            assertTrue(query.resourcesTerminated());
            query.close();
            assertTrue(query.toString().endsWith(", closed]"));
            assertFalse(query.toString().contains(KEY));
        } finally {
            release.countDown();
        }
    }

    @Test
    void concurrentRequestsCompleteIndependently() {
        responder = request -> {
            String cid = JsonParser.parseString(request.body())
                    .getAsJsonObject()
                    .get("cid")
                    .getAsString();
            var row = new JsonObject();
            row.addProperty("cid", cid);
            row.addProperty("channel_name", "c" + cid);
            var body = new JsonArray();
            body.add(row);
            var status = new JsonObject();
            status.addProperty("code", 0);
            var reply = new JsonObject();
            reply.add("body", body);
            reply.add("status", status);
            return new Reply(200, reply.toString());
        };
        try (var query = new TS3WebQuery(config().setRequestCapacity(64).build())) {
            var futures = new ArrayList<CompletableFuture<String>>();
            for (int i = 1; i <= 50; i++) {
                int cid = i;
                futures.add(CommandFutures.map(
                        query.getAsyncApi(1).channels().info(cid), info -> cid + "=" + info.getName()));
            }
            for (int i = 1; i <= 50; i++) {
                assertEquals(i + "=c" + i, CommandFutures.join(futures.get(i - 1)));
            }
        }
    }

    private static Version awaitCapacity(TS3WebQuery query) throws InterruptedException {
        // Capacity is released after the previous request's completion callbacks have run.
        for (int attempt = 0; ; attempt++) {
            try {
                return query.getApi().instance().version();
            } catch (TS3QueueFullException e) {
                if (attempt > 100) throw e;
                Thread.sleep(20);
            }
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            var _ = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
