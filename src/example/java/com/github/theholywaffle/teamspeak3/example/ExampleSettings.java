package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.FileTransferConfig;
import com.github.theholywaffle.teamspeak3.SshHostKeyPolicy;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import com.github.theholywaffle.teamspeak3.WebQueryConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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
                .setEnableCommunicationsLogging(flag("TS3_COMMUNICATIONS_LOGGING"));
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

    /**
     * @return a Web Query configuration for {@code TS3_WEBQUERY_URI} and {@code TS3_API_KEY}, trusting only
     *     the PEM certificates in {@code TS3_WEBQUERY_CERTIFICATE} when set, and accepting plain HTTP only
     *     when {@code TS3_WEBQUERY_PLAINTEXT} is {@code true}
     * @throws IllegalArgumentException if a required variable is missing or a value is invalid
     */
    public WebQueryConfig webQueryConfig() {
        String endpoint = required("TS3_WEBQUERY_URI");
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw unsupported("TS3_WEBQUERY_URI");
        }
        WebQueryConfig.Builder builder = WebQueryConfig.builder(uri, required("TS3_API_KEY"))
                .setPlaintextHttpAllowed(flag("TS3_WEBQUERY_PLAINTEXT"));
        optional("TS3_WEBQUERY_CERTIFICATE").ifPresent(file -> builder.setSslContext(trusting(Path.of(file))));
        return builder.build();
    }

    private static SSLContext trusting(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
            trust.load(null, null);
            List<? extends Certificate> certificates =
                    List.copyOf(CertificateFactory.getInstance("X.509").generateCertificates(in));
            if (certificates.isEmpty())
                throw new IllegalArgumentException("TS3_WEBQUERY_CERTIFICATE contains no certificate");
            for (int i = 0; i < certificates.size(); i++) {
                trust.setCertificateEntry("certificate-" + i, certificates.get(i));
            }
            TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            managers.init(trust);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, managers.getTrustManagers(), null);
            return context;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("TS3_WEBQUERY_CERTIFICATE is not a PEM certificate", e);
        }
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

    private boolean flag(String name) {
        return Boolean.parseBoolean(optional(name).orElse("false"));
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
