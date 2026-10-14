package consumer;

import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3WebQuery;
import com.github.theholywaffle.teamspeak3.WebQueryConfig;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * A consumer of the staged library: a RAW query and a Web Query request against loopback peers, an SSH
 * attempt that loads the runtime-scoped crypto provider, and library logging through the provider this
 * application chose.
 *
 * <p>Usage: {@code ConsumerApp simple|jdk14}. Prints the library JAR location for checksum comparison.
 */
public final class ConsumerApp {
    private ConsumerApp() {}

    public static void main(String[] args) throws Exception {
        List<String> captured = new CopyOnWriteArrayList<>();
        Path simpleLog = null;
        String expectedProvider;
        switch (args.length == 1 ? args[0] : "") {
            case "simple" -> {
                simpleLog = Files.createTempFile("consumer-slf4j-simple", ".log");
                System.setProperty("org.slf4j.simpleLogger.logFile", simpleLog.toString());
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                expectedProvider = "org.slf4j.simple.SimpleServiceProvider";
            }
            case "jdk14" -> {
                captureJulRecords(captured);
                expectedProvider = "org.slf4j.jul.JULServiceProvider";
            }
            default -> throw new IllegalArgumentException("Usage: ConsumerApp simple|jdk14");
        }

        List<String> providers = ServiceLoader.load(SLF4JServiceProvider.class).stream()
                .map(provider -> provider.type().getName())
                .toList();
        check(providers.equals(List.of(expectedProvider)), "Exactly the application's provider: " + providers);

        check("consumer-probe".equals(rawVersion()), "Typed RAW response");
        check("consumer-web".equals(webQueryVersion()), "Typed Web Query response");
        sshAttemptLoadsCryptoProvider();

        if (simpleLog != null) {
            captured.addAll(Files.readAllLines(simpleLog));
            Files.delete(simpleLog);
        }
        check(captured.stream().anyMatch(line -> line.contains("[version] > version")), "Library log routed");

        System.out.println("library="
                + Path.of(TS3Query.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()));
        System.out.println("module=" + TS3Query.class.getModule().getName());
        System.out.println("CONSUMER OK");
    }

    private static String rawVersion() throws Exception {
        try (var peer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(5000);
            CompletableFuture<Void> script = CompletableFuture.runAsync(() -> {
                try (var socket = peer.accept();
                        var in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                    OutputStream out = socket.getOutputStream();
                    expect(in, "version");
                    out.write("TS3\nversion=consumer-probe build=1 platform=test\nerror id=0 msg=ok\n"
                            .getBytes(StandardCharsets.UTF_8));
                    expect(in, "quit");
                    out.write("error id=0 msg=ok\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            var config = new TS3Config()
                    .setHost("127.0.0.1")
                    .setQueryPort(peer.getLocalPort())
                    .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                    .setEnableCommunicationsLogging(true)
                    .setCommandResponseTimeout(Duration.ofSeconds(5));
            try (var query = new TS3Query(config)) {
                query.connect();
                String version = query.getApi().instance().version().version();
                query.exit();
                script.get(5, TimeUnit.SECONDS);
                return version;
            }
        }
    }

    private static String webQueryVersion() throws Exception {
        try (var peer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(5000);
            CompletableFuture<Void> script = CompletableFuture.runAsync(() -> {
                try (var socket = peer.accept();
                        var in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                    expect(in, "POST /version HTTP/1.1");
                    int length = -1;
                    boolean key = false;
                    for (String header; !(header = in.readLine()).isEmpty(); ) {
                        String lower = header.toLowerCase(Locale.ROOT);
                        if (lower.equals("x-api-key: consumer-key")) key = true;
                        if (lower.startsWith("content-length: ")) length = Integer.parseInt(header.substring(16));
                    }
                    check(key, "API key header");
                    check(length == 2 && in.read() == '{' && in.read() == '}', "Empty JSON body");
                    byte[] body = "{\"body\":[{\"version\":\"consumer-web\",\"build\":\"1\",\"platform\":\"test\"}],"
                            .concat("\"status\":{\"code\":0,\"message\":\"ok\"}}")
                            .getBytes(StandardCharsets.UTF_8);
                    OutputStream out = socket.getOutputStream();
                    out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length
                                    + "\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    out.write(body);
                    out.flush();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            var config = WebQueryConfig.builder(URI.create("http://127.0.0.1:" + peer.getLocalPort()), "consumer-key")
                    .setPlaintextHttpAllowed(true)
                    .build();
            try (var query = new TS3WebQuery(config)) {
                String version = query.getApi().instance().version().version();
                script.get(5, TimeUnit.SECONDS);
                return version;
            }
        }
    }

    private static void sshAttemptLoadsCryptoProvider() throws Exception {
        try (var peer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(5000);
            CompletableFuture<Void> refuse = CompletableFuture.runAsync(() -> {
                try (var socket = peer.accept()) {
                    socket.getOutputStream().write("not-ssh\r\n".getBytes(StandardCharsets.US_ASCII));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            var config = new TS3Config()
                    .setHost("127.0.0.1")
                    .setQueryPort(peer.getLocalPort())
                    .setProtocol(TS3Query.Protocol.SSH)
                    .setLoginCredentials("consumer", "consumer")
                    .setHandshakeTimeout(Duration.ofSeconds(5));
            try (var query = new TS3Query(config)) {
                query.connect();
                throw new IllegalStateException("SSH must reject a non-SSH peer");
            } catch (TS3ConnectionFailedException expected) {
                refuse.get(5, TimeUnit.SECONDS);
            }
        }
        check(Security.getProvider("BC") != null, "SSHJ registered the Bouncy Castle provider");
    }

    private static void captureJulRecords(List<String> captured) {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                captured.add(logRecord.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
    }

    private static void expect(BufferedReader in, String command) throws Exception {
        String line = in.readLine();
        if (!command.equals(line)) throw new IllegalStateException("Expected " + command + " but got " + line);
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new IllegalStateException("Consumer check failed: " + description);
    }
}
