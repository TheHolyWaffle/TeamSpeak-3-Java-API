package fixture;

import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Full UDP client, controlled only inside a disposable test network. No audio source or desktop. */
public final class RegularClient {
    private static final Set<String> COMMANDS = Set.of("tokenuse", "messagelist", "messageget", "messageupdateflag", "messagedel", "clientmove");

    private record Notification(String kind, String message) { }

    public static void main(String[] args) throws Exception {
        var client = new LocalTeamspeakClientSocket();
        client.setIdentity(LocalIdentity.generateNew(8));
        client.setNickname("issue441-regular-fixture");
        var failed = new java.util.concurrent.atomic.AtomicBoolean();
        var notifications = new java.util.concurrent.ArrayBlockingQueue<Notification>(16);
        client.addListener(new com.github.manevolent.ts3j.event.TS3Listener() {
            private void publish(String kind, String message) {
                if (!notifications.offer(new Notification(kind, message))) failed.set(true);
            }
            @Override public void onClientPoke(com.github.manevolent.ts3j.event.ClientPokeEvent event) { publish("poke", event.getMessage()); }
            @Override public void onTextMessage(com.github.manevolent.ts3j.event.TextMessageEvent event) { publish("text", event.getMessage()); }
        });
        var server = HttpServer.create(new InetSocketAddress(18080), 0);
        server.createContext("/", exchange -> {
            String result; int status = 200;
            try {
                String path = exchange.getRequestURI().getPath();
                if (!path.equals("/ready") && !System.getenv("FIXTURE_CONTROL_TOKEN").equals(exchange.getRequestHeaders().getFirst("X-Fixture-Token"))) {
                    exchange.sendResponseHeaders(403, -1); exchange.close(); return;
                }
                if (failed.get()) throw new IllegalStateException("fixture session failed");
                if (!path.equals("/ready") && !exchange.getRequestMethod().equals("POST")) throw new IllegalArgumentException("POST required");
                if (path.equals("/ready")) result = "ready";
                else if (path.equals("/connect")) {
                    client.connect(new InetSocketAddress(args[0], 9987), null, 10000L);
                    result = "connected";
                } else if (path.equals("/disconnect")) {
                    client.disconnect("fixture disconnect"); result = "disconnected";
                } else if (path.equals("/notification")) {
                    byte[] input = exchange.getRequestBody().readNBytes(8193);
                    if (input.length > 8192) throw new IllegalArgumentException("notification request too long");
                    String[] expected = new String(input, StandardCharsets.UTF_8).split("\n", -1);
                    if (expected.length != 2 || !Set.of("poke", "text").contains(expected[0])) throw new IllegalArgumentException("invalid notification request");
                    var wanted = new Notification(expected[0], new String(java.util.Base64.getDecoder().decode(expected[1]), StandardCharsets.UTF_8));
                    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                    Notification observed;
                    do {
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0) throw new java.util.concurrent.TimeoutException();
                        observed = notifications.poll(remaining, java.util.concurrent.TimeUnit.NANOSECONDS);
                        if (failed.get()) throw new IllegalStateException("notification overflow");
                        if (observed == null) throw new java.util.concurrent.TimeoutException();
                    } while (!observed.equals(wanted));
                    result = observed.kind() + "\n" + java.util.Base64.getEncoder().encodeToString(observed.message().getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/command")) {
                    byte[] input = exchange.getRequestBody().readNBytes(8193);
                    if (input.length > 8192) throw new IllegalArgumentException("command too long");
                    String text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(input)).toString();
                    if (text.contains("\n") || text.contains("\r")) throw new IllegalArgumentException("invalid command");
                    var command = SingleCommand.parse(ProtocolRole.CLIENT, text);
                    if (!COMMANDS.contains(command.getName())) throw new IllegalArgumentException("command not permitted");
                    var response = new StringBuilder();
                    for (var row : client.executeCommand(command).get(10000L)) response.append(row.toString()).append('\n');
                    result = response.toString();
                } else { status = 404; result = "unknown endpoint"; }
            } catch (java.util.concurrent.TimeoutException failure) {
                failed.set(true);
                try { client.close(); } catch (Exception ignored) { }
                status = 500; result = "fixture deadline exceeded; session closed";
            } catch (com.github.manevolent.ts3j.command.CommandException failure) {
                status = 500; result = "CommandException id=" + failure.getErrorId();
            } catch (Error failure) {
                failed.set(true);
                status = 500; result = failure.getClass().getSimpleName();
            } catch (Exception failure) {
                status = 500;
                // Neither identities nor command values (including privilege keys) belong in logs.
                result = failure.getClass().getSimpleName();
            }
            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(0); try { client.close(); } catch (Exception ignored) { } }));
        System.out.println("regular-client control ready");
    }
}
