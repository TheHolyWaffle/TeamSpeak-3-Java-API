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
                .setProtocol(
                        switch (lowerCase("TS3_PROTOCOL", "raw")) {
                            case "raw" -> Protocol.RAW;
                            case "ssh" -> Protocol.SSH;
                            default -> throw unsupported("TS3_PROTOCOL");
                        })
                .setLoginCredentials(required("TS3_USERNAME"), required("TS3_PASSWORD"))
                .setFloodRate(
                        switch (lowerCase("TS3_FLOOD_RATE", "default")) {
                            case "default" -> FloodRate.DEFAULT;
                            case "unlimited" -> FloodRate.UNLIMITED;
                            default -> throw unsupported("TS3_FLOOD_RATE");
                        })
                .setEnableCommunicationsLogging(Boolean.parseBoolean(
                        optional("TS3_COMMUNICATIONS_LOGGING").orElse("false")));
        optional("TS3_QUERY_PORT").ifPresent(port -> config.setQueryPort(port("TS3_QUERY_PORT")));
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
            int port = port("TS3_FILE_TRANSFER_PORT");
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
        return integer("TS3_VIRTUAL_SERVER_ID", "1", Integer.MAX_VALUE);
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

    private String required(String name) {
        return optional(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Set the " + name + " environment variable (see the README's \"Run an example\")"));
    }

    private Optional<String> optional(String name) {
        return Optional.ofNullable(environment.get(name)).filter(value -> !value.isBlank());
    }

    private String lowerCase(String name, String fallback) {
        return optional(name).orElse(fallback).trim().toLowerCase(Locale.ROOT);
    }

    private IllegalArgumentException unsupported(String name) {
        return new IllegalArgumentException(name + " has an unsupported value: " + environment.get(name));
    }

    private int port(String name) {
        return integer(name, "", 65535);
    }

    private int integer(String name, String fallback, int max) {
        String value = optional(name).orElse(fallback).trim();
        try {
            int number = Integer.parseInt(value);
            if (number > 0 && number <= max) return number;
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw new IllegalArgumentException(name + " must be an integer between 1 and " + max + ": " + value);
    }
}
