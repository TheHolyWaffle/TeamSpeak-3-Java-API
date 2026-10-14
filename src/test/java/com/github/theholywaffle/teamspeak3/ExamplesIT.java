package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.theholywaffle.teamspeak3.TS3Query.Protocol;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Runs every compiled example, unchanged, against a disposable server using only TS3_* variables. */
class ExamplesIT {
    @RegisterExtension
    static final TeamSpeakContainer SERVER =
            new TeamSpeakContainer().withFileTransferPort().withWebQuery();

    private static final Path EXAMPLES = Path.of("src/example/java/com/github/theholywaffle/teamspeak3/example");

    /** Output proving each example completed its work; every example must be listed. */
    private static final Map<String, String> COMPLETED = Map.of(
            "ChatBotExample", "Running. Press Enter",
            "ClientInfoExample", "Server version: " + TeamSpeakContainer.TS3_VERSION,
            "CreateChannelExample", "Deleted channel",
            "EventListenerExample", "Event statistics: ",
            "FileTransferExample", "Stream download (30 bytes): Hello file transferring world!",
            "ReconnectExample", "Deleted channels [",
            "TrollExample", "Waiting for clients to join.",
            "WebQueryExample", "Events need a RAW or SSH query: ");

    @TempDir
    static Path trust;

    static Path knownHosts;

    static String apiKey;

    @BeforeAll
    static void trustServer() throws IOException {
        knownHosts = SERVER.writeKnownHosts(trust.resolve("known_hosts"));
        apiKey = SERVER.createWebQueryKey();
    }

    /** RAW and SSH run the ServerQuery examples; HTTP and HTTPS run the Web Query example. */
    static Map<String, String> environment(String example, String transport) throws IOException {
        if (!example.equals("WebQueryExample"))
            return SERVER.exampleEnvironment(Protocol.valueOf(transport), knownHosts);
        return SERVER.webQueryEnvironment(transport.equals("HTTPS"), apiKey, trust.resolve("webquery.pem"));
    }

    static Stream<Arguments> examples() throws IOException {
        Set<String> examples;
        try (Stream<Path> files = Files.list(EXAMPLES)) {
            examples = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith("Example.java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(Collectors.toSet());
        }
        assertEquals(COMPLETED.keySet(), examples, "Every example needs an expected completion line");
        return examples.stream()
                .sorted()
                .flatMap(example -> (example.equals("WebQueryExample")
                                ? Stream.of("HTTP", "HTTPS")
                                : Stream.of(Protocol.RAW.name(), Protocol.SSH.name()))
                        .map(transport -> Arguments.of(example, transport)));
    }

    @ParameterizedTest(name = "{0} over {1}")
    @MethodSource("examples")
    void runsUnchanged(String example, String transport) throws Exception {
        String output = ForkedJvm.runExample(example, environment(example, transport), 0);
        assertTrue(output.contains(COMPLETED.get(example)), output);
    }

    @Test
    void chatBotAnswersPing() throws Exception {
        try (ForkedJvm bot = ForkedJvm.example("ChatBotExample", SERVER.exampleEnvironment(Protocol.RAW, knownHosts));
                TS3Query query = new TS3Query(SERVER.config(Protocol.RAW))) {
            bot.awaitOutput("Running.", Duration.ofSeconds(60));
            query.connect();
            TS3Api api = query.getApi();
            api.selectVirtualServerById(1);
            CompletableFuture<String> reply = new CompletableFuture<>();
            api.events().subscribe(new TS3EventAdapter() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    if (e.getMessage().equals("pong"))
                        reply.complete(e.getInvokerName().orElseThrow());
                }
            });
            api.events().register(TS3EventType.TEXT_CHANNEL);
            api.messages().sendChannel("!ping");
            assertEquals("Java API example", reply.get(30, TimeUnit.SECONDS));
            bot.closeInput();
            assertEquals(0, bot.await(Duration.ofSeconds(30)), bot.output());
        }
    }

    @Test
    void missingSettingsFailWithoutConnecting() throws Exception {
        String output = ForkedJvm.runExample("ClientInfoExample", Map.of(), 1);
        assertTrue(output.contains("Set the TS3_HOST environment variable"), output);
        String web = ForkedJvm.runExample("WebQueryExample", Map.of(), 1);
        assertTrue(web.contains("Set the TS3_WEBQUERY_URI environment variable"), web);
    }
}
