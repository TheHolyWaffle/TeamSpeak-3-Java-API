package com.github.theholywaffle.teamspeak3.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.theholywaffle.teamspeak3.FileTransferEndpointResolver;
import com.github.theholywaffle.teamspeak3.QueryConfig;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExampleSettingsTest {
    private static final Map<String, String> REQUIRED =
            Map.of("TS3_HOST", "192.0.2.10", "TS3_USERNAME", "bot", "TS3_PASSWORD", "secret");

    private static ExampleSettings with(String... pairs) {
        Map<String, String> environment = new HashMap<>(REQUIRED);
        for (int i = 0; i < pairs.length; i += 2) environment.put(pairs[i], pairs[i + 1]);
        return new ExampleSettings(environment);
    }

    @Test
    void defaultsToRawLoginWithFloodProtection() {
        QueryConfig config = with().config().build();
        assertEquals("192.0.2.10", config.getHost());
        assertEquals(Protocol.RAW, config.getProtocol());
        assertEquals(10011, config.getQueryPort());
        assertEquals("bot", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertEquals(FloodRate.DEFAULT, config.getFloodRate());
        assertFalse(config.getEnableCommunicationsLogging());
        assertEquals(1, with().virtualServerId());
        assertEquals("Java API example", with().nickname());
        assertEndpoint(
                "192.0.2.10",
                30033,
                config.getFileTransferConfig().endpointResolver().resolve("192.0.2.10", "0.0.0.0", 30033));
    }

    @Test
    void appliesOptionalSettings() {
        ExampleSettings settings = with(
                "TS3_PROTOCOL", "ssh",
                "TS3_QUERY_PORT", "41022",
                "TS3_FLOOD_RATE", "unlimited",
                "TS3_COMMUNICATIONS_LOGGING", "true",
                "TS3_SSH_KNOWN_HOSTS", "trust/known_hosts",
                "TS3_FILE_TRANSFER_HOST", "198.51.100.7",
                "TS3_FILE_TRANSFER_PORT", "43033",
                "TS3_VIRTUAL_SERVER_ID", "7",
                "TS3_NICKNAME", "Bot");
        QueryConfig config = settings.config().build();
        assertEquals(Protocol.SSH, config.getProtocol());
        assertEquals(41022, config.getQueryPort());
        assertEquals(FloodRate.UNLIMITED, config.getFloodRate());
        assertTrue(config.getEnableCommunicationsLogging());
        assertEquals(
                "SshHostKeyPolicy[KNOWN_HOSTS]", config.getSshHostKeyPolicy().toString());
        FileTransferEndpointResolver resolver = config.getFileTransferConfig().endpointResolver();
        assertEndpoint("198.51.100.7", 43033, resolver.resolve("q", "10.0.0.2", 30033));
        assertEquals(7, settings.virtualServerId());
        assertEquals("Bot", settings.nickname());
    }

    private static void assertEndpoint(String host, int port, InetSocketAddress endpoint) {
        assertEquals(host, endpoint.getHostString());
        assertEquals(port, endpoint.getPort());
    }

    @ParameterizedTest
    @CsvSource({"TS3_HOST", "TS3_USERNAME", "TS3_PASSWORD"})
    void requiresExplicitConnectionSettings(String name) {
        Map<String, String> environment = new HashMap<>(REQUIRED);
        environment.put(name, " ");
        var failure = assertThrows(IllegalArgumentException.class, () -> new ExampleSettings(environment).config());
        assertTrue(failure.getMessage().contains("Set the " + name + " environment variable"));
    }

    @ParameterizedTest
    @CsvSource({
        "TS3_PROTOCOL, web",
        "TS3_QUERY_PORT, 0",
        "TS3_QUERY_PORT, 70000",
        "TS3_QUERY_PORT, ten",
        "TS3_FLOOD_RATE, fast",
        "TS3_FILE_TRANSFER_HOST, 198.51.100.7"
    })
    void rejectsInvalidSettings(String name, String value) {
        assertThrows(IllegalArgumentException.class, () -> with(name, value).config());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "one"})
    void rejectsInvalidVirtualServerIds(String value) {
        assertThrows(
                IllegalArgumentException.class,
                () -> with("TS3_VIRTUAL_SERVER_ID", value).virtualServerId());
    }
}
