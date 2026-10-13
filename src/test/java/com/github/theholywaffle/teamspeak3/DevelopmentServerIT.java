package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.api.exception.NotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.DockerClientFactory;

/** Exercise the real forked launcher, including its JVM shutdown hook. */
class DevelopmentServerIT {
    /** Follows the printed instructions: the raw exports, then the SSH exports on top of them. */
    private static void runExamplesFromPrintedExports(List<String> output) throws Exception {
        List<Map<String, String>> exports = output.stream()
                .map(String::trim)
                .filter(line -> line.startsWith(DevelopmentServer.EXPORT_PREFIX))
                .map(DevelopmentServerIT::parseExports)
                .toList();
        assertEquals(2, exports.size(), output.toString());
        Map<String, String> ssh = new HashMap<>(exports.get(0));
        ssh.putAll(exports.get(1));
        for (Map<String, String> environment : List.of(exports.get(0), ssh)) {
            String result = ForkedJvm.runExample("ClientInfoExample", environment, 0);
            assertTrue(result.contains("Server version: 3.13.8"), result);
        }
    }

    private static Map<String, String> parseExports(String line) {
        Map<String, String> variables = new HashMap<>();
        Matcher matcher = Pattern.compile("(TS3_[A-Z_]+)='([^']*)'").matcher(line);
        while (matcher.find()) variables.put(matcher.group(1), matcher.group(2));
        return variables;
    }

    @ParameterizedTest(name = "{0}: launcher example and {1} cleanup")
    @CsvSource({"ts3,ENTER", "ts3,EOF", "ts3,SIGNAL", "ts3,ONCE"})
    void exampleAndCleanup(String server, String stop) throws Exception {
        assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Launcher validation requires Docker");
        try (ForkedJvm launcher = ForkedJvm.start(
                DevelopmentServer.class.getName(), Map.of(), server, Boolean.toString(stop.equals("ONCE")))) {
            if (!stop.equals("ONCE")) {
                launcher.awaitOutput("Press Enter", Duration.ofMinutes(2));
                if (stop.equals("ENTER")) runExamplesFromPrintedExports(launcher.lines());
                switch (stop) {
                    case "ENTER" -> launcher.pressEnter();
                    case "EOF" -> launcher.closeInput();
                    case "SIGNAL" -> launcher.terminate();
                    default -> fail("Unknown stop mode");
                }
            }
            int exit = launcher.await(Duration.ofMinutes(2));
            List<String> output = launcher.lines();
            if (!stop.equals("SIGNAL")) assertEquals(0, exit, output.toString());
            assertTrue(
                    output.stream()
                            .anyMatch(line -> line.startsWith("Compiled example succeeded: version=" + "3.13.8")),
                    output.toString());
            String ready = output.stream()
                    .filter(line -> line.startsWith("Development server ready:"))
                    .findFirst()
                    .orElseThrow();
            String id = ready.substring(ready.indexOf("container=") + "container=".length());
            assertFalse(
                    Files.exists(Path.of("target", "dev-server", id)),
                    "Stopping the launcher must remove its SSH trust");
            assertThrows(
                    NotFoundException.class,
                    () -> DockerClientFactory.instance()
                            .client()
                            .inspectContainerCmd(id)
                            .exec(),
                    "Stopping the launcher must remove its container");
        }
    }
}
