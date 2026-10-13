package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps the README configuration reference and the Markdown links in step with the code. */
class DocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)\\)");

    @Test
    void configurationTableListsEverySetter() throws IOException {
        String readme = Files.readString(README);
        String table = readme.substring(readme.indexOf("## Configuration reference"), readme.indexOf("## Development"));
        Set<String> documented = Pattern.compile("^\\| `(set\\w+)\\(", Pattern.MULTILINE)
                .matcher(table)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        Set<String> setters = Arrays.stream(TS3Config.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .collect(Collectors.toSet());
        assertEquals(setters, documented);
    }

    @Test
    void documentedDefaultsMatchConfiguration() {
        QueryConfig config = new TS3Config().build();
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(10011, config.getQueryPort());
        assertEquals(
                10022,
                new TS3Config()
                        .setProtocol(Protocol.SSH)
                        .setLoginCredentials("u", "p")
                        .build()
                        .getQueryPort());
        assertEquals(Protocol.RAW, config.getProtocol());
        assertFalse(config.hasLoginCredentials());
        assertEquals(
                "SshHostKeyPolicy[KNOWN_HOSTS]", config.getSshHostKeyPolicy().toString());
        assertEquals(FloodRate.DEFAULT, config.getFloodRate());
        assertEquals(350, FloodRate.DEFAULT.getMs());
        assertFalse(config.getEnableCommunicationsLogging());
        for (Duration budget : List.of(
                config.getConnectTimeout(),
                config.getHandshakeTimeout(),
                config.getQueueWaitTimeout(),
                config.getCommandResponseTimeout(),
                config.getCloseTimeout())) {
            assertEquals(Duration.ofSeconds(4), budget);
        }
        assertEquals(1024, config.getCommandCapacity());
        assertEquals(64, config.getListenerCapacity());
        assertEquals(256, config.getListenerQueueCapacity());
        assertEquals(4, config.getEventCallbackThreads());
        assertFalse(config.getReconnectStrategy().isReconnectEnabled());
        assertEquals(0, config.getCommandRetryPolicy().getMaxRetries());
        assertEquals(null, config.getSessionConfiguration());
        assertEquals(null, config.getConnectionHandler());
        FileTransferConfig files = config.getFileTransferConfig();
        assertEquals(Duration.ofSeconds(4), files.connectTimeout());
        assertEquals(Duration.ofSeconds(4), files.readTimeout());
        assertEquals(Duration.ofMinutes(5), files.transferTimeout());
        assertEquals(16 * 1024 * 1024, files.maxInMemoryBytes());
        assertEquals(QueryTransports.RAW, config.getTransportFactory());
    }

    /** An image update changes the verified support claim, so it fails until the README is reviewed. */
    @Test
    void supportTableNamesThePinnedServerVersion() throws IOException {
        Set<String> documented = Pattern.compile("^\\| TeamSpeak 3 server (\\d+\\.\\d+\\.\\d+) ", Pattern.MULTILINE)
                .matcher(Files.readString(README))
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        assertEquals(
                Set.of(TeamSpeakContainer.TS3_VERSION),
                documented,
                "Update the README support table for src/test/resources/teamspeak3/Dockerfile");
    }

    @Test
    void relativeLinksAndAnchorsResolve() throws IOException {
        List<Path> pages = new ArrayList<>(List.of(README));
        try (Stream<Path> docs = Files.list(Path.of("docs"))) {
            docs.filter(file -> file.toString().endsWith(".md")).forEach(pages::add);
        }
        List<String> broken = new ArrayList<>();
        for (Path page : pages) {
            Matcher links = LINK.matcher(Files.readString(page));
            while (links.find()) {
                String link = links.group(1);
                if (link.matches("^[a-z]+:.*") || link.startsWith("../../")) continue;
                int hash = link.indexOf('#');
                String file = hash < 0 ? link : link.substring(0, hash);
                Path target = file.isEmpty() ? page : page.resolveSibling(file).normalize();
                if (!Files.exists(target)) {
                    broken.add(page + ": " + link);
                } else if (hash >= 0
                        && target.toString().endsWith(".md")
                        && !anchors(target).contains(link.substring(hash + 1))) {
                    broken.add(page + ": " + link + " (missing anchor)");
                }
            }
        }
        assertTrue(broken.isEmpty(), String.join("\n", broken));
    }

    /** GitHub heading anchors: lower case, punctuation removed, spaces as hyphens. */
    private static Set<String> anchors(Path page) throws IOException {
        return Files.readAllLines(page).stream()
                .filter(line -> line.matches("^#{1,6} .*"))
                .map(line -> line.replaceFirst("^#+ ", "")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N} _-]", "")
                        .replace(' ', '-'))
                .collect(Collectors.toSet());
    }
}
