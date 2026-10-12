package com.github.theholywaffle.teamspeak3;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

/** Isolated full-protocol fixture with a newly generated identity; never uses the host desktop. */
final class RegularClientContainer extends GenericContainer<RegularClientContainer> {
    static final String IMAGE = "eclipse-temurin:25-jre@sha256:fcd7fd7b387f94bb2ac461478a7436ad8e349924c374ea8313919624dceae636";
    private final String token = java.util.UUID.randomUUID().toString();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).version(HttpClient.Version.HTTP_1_1).build();

    RegularClientContainer(Network network) {
        super(IMAGE);
        withNetwork(network);
        withEnv("FIXTURE_CONTROL_TOKEN", token);
        withExposedPorts(18080);
        withCopyFileToContainer(MountableFile.forHostPath(Path.of("target/fixtures/regular-client/target/fixture.jar")), "/fixture.jar");
        withCommand("java", "-jar", "/fixture.jar", "server");
        waitingFor(Wait.forHttp("/ready").forPort(18080).withStartupTimeout(Duration.ofSeconds(60)));
    }

    void connect() throws Exception { request("connect", ""); }
    void disconnect() throws Exception { request("disconnect", ""); }
    String command(String text) throws Exception { return request("command", text); }

    void assertNotification(String kind, String message) throws Exception {
        String encoded = kind + "\n" + java.util.Base64.getEncoder().encodeToString(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertEquals(encoded, request("notification", encoded));
    }

    private String request(String endpoint, String body) throws Exception {
        var uri = new URI("http", null, getHost(), getMappedPort(18080), "/" + endpoint, null, null);
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(endpoint.equals("disconnect") ? 45 : 15)).header("X-Fixture-Token", token).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response;
        try { response = http.send(request, HttpResponse.BodyHandlers.ofString()); }
        catch (java.io.IOException failure) {
            String diagnostics = getLogs().lines().filter(line -> line.equals("regular-client control ready")).collect(java.util.stream.Collectors.joining("\n"));
            throw new java.io.IOException("Regular-client control failed: " + diagnostics, failure);
        }
        if (response.statusCode() != 200) throw new IllegalStateException("Regular-client " + endpoint + " failed: " + response.body() + "\n" + getLogs().lines().filter(line -> line.stripLeading().startsWith("at ")).collect(java.util.stream.Collectors.joining("\n")));
        return response.body();
    }

    @Override public void close() { try { super.close(); } finally { http.close(); } }
}
