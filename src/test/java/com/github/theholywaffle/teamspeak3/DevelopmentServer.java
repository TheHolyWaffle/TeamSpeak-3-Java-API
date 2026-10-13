package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/** Development-only entry point; never included in library artifacts. */
public final class DevelopmentServer {
    private DevelopmentServer() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].equals("ts3") || !(args[1].equals("true") || args[1].equals("false"))) {
            throw new IllegalArgumentException("Use -Ddev.server=ts3 and -Ddev.once=true|false");
        }
        try (Resources resources = new Resources(new TeamSpeakContainer().withFileTransferPort())) {
            Thread hook = new Thread(resources::close, "teamspeak-dev-cleanup");
            Runtime.getRuntime().addShutdownHook(hook);
            try {
                TeamSpeakContainer server = resources.server;
                server.start();
                System.out.println("Development server ready: " + args[0] + " container=" + server.getContainerId());
                System.out.println("Image: " + server.getDockerImageName());
                System.out.println("SSH ServerQuery: " + server.getHost() + ":" + server.getMappedPort(10022));
                System.out.println("Raw ServerQuery: " + server.getHost() + ":" + server.getMappedPort(10011));
                System.out.println(
                        "Disposable login: serveradmin / " + TeamSpeakContainer.PASSWORD + "; virtual server: 1");
                System.out.println("File transfer: " + server.getHost() + ":" + server.getMappedPort(30033));
                // Trusts only this container's key, for this session; removed when the launcher stops.
                resources.knownHosts = server.writeKnownHosts(
                        Path.of("target", "dev-server", server.getContainerId(), "known_hosts")
                                .toAbsolutePath());
                printExampleEnvironment(server, resources.knownHosts);
                DevelopmentServerExample.run(server.config(TS3Query.Protocol.SSH));
                if (!Boolean.parseBoolean(args[1])) {
                    System.out.println("Press Enter or Ctrl+C to stop and remove the server. EOF also stops it.");
                    new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
                }
            } finally {
                // Keep the hook installed until cleanup completes: SIGTERM can also close stdin.
                try {
                    resources.close();
                } finally {
                    try {
                        Runtime.getRuntime().removeShutdownHook(hook);
                    } catch (IllegalStateException shuttingDown) {
                        /* Shutdown is already running the hook. */
                    }
                }
            }
        }
    }

    private static void printExampleEnvironment(TeamSpeakContainer server, Path knownHosts) {
        Map<String, String> raw = server.exampleEnvironment(TS3Query.Protocol.RAW, knownHosts);
        Map<String, String> ssh = new LinkedHashMap<>(server.exampleEnvironment(TS3Query.Protocol.SSH, knownHosts));
        ssh.entrySet().removeIf(variable -> variable.getValue().equals(raw.get(variable.getKey())));
        System.out.println();
        System.out.println("Run an example from another terminal in this directory (POSIX shell):");
        System.out.println("  " + EXPORT_PREFIX + exports(raw));
        System.out.println("  ./mvnw -q -ntp -Pexample test-compile exec:exec -Dexample=ClientInfoExample");
        System.out.println("To use SSH instead of raw TCP, also run:");
        System.out.println("  " + EXPORT_PREFIX + exports(ssh));
        System.out.println();
    }

    static final String EXPORT_PREFIX = "export ";

    private static String exports(Map<String, String> variables) {
        StringJoiner line = new StringJoiner(" ");
        variables.forEach((name, value) -> line.add(name + "='" + value.replace("'", "'\\''") + "'"));
        return line.toString();
    }

    private static final class Resources implements AutoCloseable {
        final TeamSpeakContainer server;
        volatile Path knownHosts;
        boolean closed;

        Resources(TeamSpeakContainer server) {
            this.server = server;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            server.close();
            if (knownHosts != null) {
                try {
                    Files.deleteIfExists(knownHosts);
                    Files.deleteIfExists(knownHosts.getParent());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            System.out.println("Development server removed; disposable data and SSH trust reset.");
        }
    }
}
