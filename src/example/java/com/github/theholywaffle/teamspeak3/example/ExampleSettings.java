package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.FileTransferConfig;
import com.github.theholywaffle.teamspeak3.SshHostKeyPolicy;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Connection settings shared by the examples, read from environment variables so no example
 * contains a host or credential. See the README's "Run an example" section for every variable.
 */
public final class ExampleSettings {

    private final Map<String, String> environment;

    ExampleSettings(Map<String, String> environment) {
        this.environment = Map.copyOf(environment);
    }

    /** @return settings read from the process environment */
    public static ExampleSettings fromEnvironment() {
        return new ExampleSettings(System.getenv());
    }

    /**
     * @return a new configuration for {@code TS3_HOST}, {@code TS3_PROTOCOL}, {@code TS3_QUERY_PORT},
     *     the login, flood rate, SSH trust file and optional file-transfer endpoint
     * @throws IllegalArgumentException if a required variable is missing or a value is invalid
     */
    public TS3Config config() {
        TS3Config config = new TS3Config()
                .setHost(required("TS3_HOST"))
                .setProtocol(choice("TS3_PROTOCOL", Protocol.class, Protocol.RAW))
                .setLoginCredentials(required("TS3_USERNAME"), required("TS3_PASSWORD"))
                .setFloodRate(
                        choice("TS3_FLOOD_RATE", Rate.class, Rate.DEFAULT) == Rate.UNLIMITED
                                ? FloodRate.UNLIMITED
                                : FloodRate.DEFAULT)
                .setEnableCommunicationsLogging(Boolean.parseBoolean(
                        optional("TS3_COMMUNICATIONS_LOGGING").orElse("false")));
        optional("TS3_QUERY_PORT").ifPresent(port -> config.setQueryPort(port("TS3_QUERY_PORT", port)));
        optional("TS3_SSH_KNOWN_HOSTS")
                .ifPresent(file -> config.setSshHostKeyPolicy(SshHostKeyPolicy.knownHosts(Path.of(file))));
        Optional<String> fileHost = optional("TS3_FILE_TRANSFER_HOST");
        Optional<String> filePort = optional("TS3_FILE_TRANSFER_PORT");
        if (fileHost.isPresent() != filePort.isPresent()) {
            throw new IllegalArgumentException(
                    "Set both TS3_FILE_TRANSFER_HOST and TS3_FILE_TRANSFER_PORT, or neither");
        }
        if (fileHost.isPresent()) {
            String host = fileHost.get();
            int port = port("TS3_FILE_TRANSFER_PORT", filePort.get());
            FileTransferConfig defaults = FileTransferConfig.defaults();
            config.setFileTransferConfig(new FileTransferConfig(
                    defaults.connectTimeout(),
                    defaults.readTimeout(),
                    defaults.transferTimeout(),
                    defaults.maxInMemoryBytes(),
                    (queryHost, advertisedHost, advertisedPort) -> new InetSocketAddress(host, port)));
        }
        return config;
    }

    /** @return {@code TS3_VIRTUAL_SERVER_ID}, default 1 */
    public int virtualServerId() {
        String value = optional("TS3_VIRTUAL_SERVER_ID").orElse("1");
        try {
            int id = Integer.parseInt(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw new IllegalArgumentException("TS3_VIRTUAL_SERVER_ID must be a positive integer: " + value);
    }

    /** @return {@code TS3_NICKNAME}, default "Java API example" */
    public String nickname() {
        return optional("TS3_NICKNAME").orElse("Java API example");
    }

    /**
     * Blocks until Enter or end of input, so long-running examples stop cleanly in a terminal and
     * exit immediately when standard input is closed.
     */
    public static void awaitStop() {
        System.out.println("Running. Press Enter (or close standard input) to stop.");
        try {
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private enum Rate {
        DEFAULT,
        UNLIMITED
    }

    private String required(String name) {
        return optional(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Set the " + name + " environment variable (see the README's \"Run an example\")"));
    }

    private Optional<String> optional(String name) {
        return Optional.ofNullable(environment.get(name)).filter(value -> !value.isBlank());
    }

    private <E extends Enum<E>> E choice(String name, Class<E> type, E fallback) {
        Optional<String> value = optional(name);
        if (value.isEmpty()) return fallback;
        try {
            return Enum.valueOf(type, value.get().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " has an unsupported value: " + value.get(), e);
        }
    }

    private static int port(String name, String value) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port > 0 && port <= 65535) return port;
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw new IllegalArgumentException(name + " must be a port between 1 and 65535: " + value);
    }
}
