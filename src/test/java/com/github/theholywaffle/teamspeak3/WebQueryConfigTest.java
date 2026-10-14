package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebQueryConfigTest {

    private static final String KEY = "BAByFoiEXZfnSJyE6dbXFiW_nn_SdwkclpKNz9j";

    @Test
    void documentedDefaults() {
        var config = WebQueryConfig.builder(URI.create("https://ts.example.org:10443"), KEY)
                .build();
        assertEquals(URI.create("https://ts.example.org:10443/"), config.getBaseUri());
        assertEquals(Duration.ofSeconds(4), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(4), config.getRequestTimeout());
        assertEquals(Duration.ofSeconds(4), config.getCloseTimeout());
        assertEquals(16, config.getRequestCapacity());
        assertEquals(Optional.empty(), config.getSslContext());
        assertFalse(config.isPlaintextHttpAllowed());
        assertEquals(KEY, config.apiKey());
    }

    @Test
    void keepsAProxyPathPrefixAndNormalizesTheScheme() {
        assertEquals(
                URI.create("https://proxy.example.org/team%20speak/"),
                WebQueryConfig.builder(URI.create("HTTPS://proxy.example.org/team%20speak"), KEY)
                        .build()
                        .getBaseUri());
        assertEquals(
                URI.create("http://[::1]:10080/"),
                WebQueryConfig.builder(URI.create("http://[::1]:10080"), KEY)
                        .setPlaintextHttpAllowed(true)
                        .build()
                        .getBaseUri());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ftp://ts.example.org/",
                "ts.example.org:10080",
                "/relative",
                "https:///nohost",
                "https://user:pw@ts.example.org/",
                "https://ts.example.org/?api-key=x",
                "https://ts.example.org/#fragment",
                "mailto:admin@example.org"
            })
    void rejectsEndpointsThatAreNotPlainHttpOrigins(String uri) {
        assertThrows(IllegalArgumentException.class, () -> WebQueryConfig.builder(URI.create(uri), KEY));
    }

    @Test
    void requiresAnExplicitOptInForPlaintextHttp() {
        var builder = WebQueryConfig.builder(URI.create("http://127.0.0.1:10080"), KEY);
        var error = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(error.getMessage().contains("setPlaintextHttpAllowed"));
        assertTrue(builder.setPlaintextHttpAllowed(true).build().isPlaintextHttpAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "with space", "line\nbreak", "tab\t", "nön-ascii", "\u007f"})
    void rejectsKeysThatCannotBeSentAsAHeader(String key) {
        assertThrows(
                IllegalArgumentException.class,
                () -> WebQueryConfig.builder(URI.create("https://ts.example.org"), key));
    }

    @Test
    void rejectsMissingArgumentsAndInvalidLimits() {
        URI uri = URI.create("https://ts.example.org");
        assertThrows(NullPointerException.class, () -> WebQueryConfig.builder(null, KEY));
        assertThrows(NullPointerException.class, () -> WebQueryConfig.builder(uri, null));
        var builder = WebQueryConfig.builder(uri, KEY);
        for (Duration invalid : new Duration[] {Duration.ZERO, Duration.ofMillis(-1)}) {
            assertThrows(IllegalArgumentException.class, () -> builder.setConnectTimeout(invalid));
            assertThrows(IllegalArgumentException.class, () -> builder.setRequestTimeout(invalid));
            assertThrows(IllegalArgumentException.class, () -> builder.setCloseTimeout(invalid));
        }
        assertThrows(NullPointerException.class, () -> builder.setRequestTimeout(null));
        assertThrows(IllegalArgumentException.class, () -> builder.setRequestCapacity(0));
        assertThrows(NullPointerException.class, () -> builder.setSslContext(null));
    }

    @Test
    void snapshotsTheBuilder() throws Exception {
        SSLContext context = SSLContext.getDefault();
        var builder = WebQueryConfig.builder(URI.create("https://ts.example.org"), KEY)
                .setConnectTimeout(Duration.ofSeconds(1))
                .setRequestTimeout(Duration.ofSeconds(2))
                .setCloseTimeout(Duration.ofSeconds(3))
                .setRequestCapacity(5)
                .setSslContext(context);
        var config = builder.build();
        builder.setRequestCapacity(9).setRequestTimeout(Duration.ofSeconds(9));
        assertEquals(Duration.ofSeconds(1), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(2), config.getRequestTimeout());
        assertEquals(Duration.ofSeconds(3), config.getCloseTimeout());
        assertEquals(5, config.getRequestCapacity());
        assertSame(context, config.getSslContext().orElseThrow());
    }

    @Test
    void neverPrintsTheApiKey() {
        var builder = WebQueryConfig.builder(URI.create("https://ts.example.org"), KEY);
        assertEquals("WebQueryConfig.Builder[baseUri=https://ts.example.org/]", builder.toString());
        assertEquals(
                "WebQueryConfig[baseUri=https://ts.example.org/]",
                builder.build().toString());
    }
}
