package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.api.exception.NotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
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
            String result = ExampleProcess.runToCompletion("ClientInfoExample", environment, 0);
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
        Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        DevelopmentServer.class.getName(),
                        server,
                        Boolean.toString(stop.equals("ONCE")))
                .redirectErrorStream(true)
                .start();
        List<String> output = new CopyOnWriteArrayList<>();
        Thread reader = Thread.ofPlatform().start(() -> {
            try (var lines =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = lines.readLine()) != null; ) output.add(line);
            } catch (Exception e) {
                output.add("Reader failure: " + e);
            }
        });
        try {
            long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
            while (process.isAlive()
                    && output.stream().noneMatch(line -> line.startsWith("Press Enter"))
                    && !stop.equals("ONCE")
                    && System.nanoTime() < deadline) Thread.sleep(100);
            if (!stop.equals("ONCE")) {
                assertTrue(output.stream().anyMatch(line -> line.startsWith("Press Enter")), output.toString());
                if (stop.equals("ENTER")) runExamplesFromPrintedExports(output);
                switch (stop) {
                    case "ENTER" -> {
                        process.getOutputStream().write('\n');
                        process.getOutputStream().flush();
                    }
                    case "EOF" -> process.getOutputStream().close();
                    case "SIGNAL" -> process.destroy();
                    default -> fail("Unknown stop mode");
                }
            }
            assertTrue(process.waitFor(120, TimeUnit.SECONDS), "Launcher did not terminate: " + output);
            reader.join(5000);
            if (!stop.equals("SIGNAL")) assertEquals(0, process.exitValue(), output.toString());
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
        } finally {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
            }
        }
    }
}
