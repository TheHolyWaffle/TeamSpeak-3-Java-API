package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** One compiled example in its own JVM, as {@code -Pexample exec:exec} runs it, with only the given TS3_* variables. */
final class ExampleProcess implements AutoCloseable {
    private final Process process;
    private final Path output;

    private ExampleProcess(Process process, Path output) {
        this.process = process;
        this.output = output;
    }

    static ExampleProcess start(String example, Map<String, String> environment) throws IOException {
        Path output = Files.createTempFile("example-", ".log");
        ProcessBuilder builder = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        "com.github.theholywaffle.teamspeak3.example." + example)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile());
        builder.environment().keySet().removeIf(name -> name.startsWith("TS3_"));
        builder.environment().putAll(environment);
        return new ExampleProcess(builder.start(), output);
    }

    /** Runs an example with closed standard input, so long-running examples stop after their setup. */
    static String runToCompletion(String example, Map<String, String> environment, int expectedExit) throws Exception {
        try (ExampleProcess process = start(example, environment)) {
            process.stop();
            int exit = process.await(Duration.ofSeconds(90));
            String output = process.output();
            assertTrue(exit == expectedExit, "Exit " + exit + " instead of " + expectedExit + ":\n" + output);
            return output;
        }
    }

    String output() {
        try {
            return Files.readString(output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void awaitOutput(String expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!output().contains(expected) && process.isAlive() && System.nanoTime() < deadline) Thread.sleep(100);
        assertTrue(output().contains(expected), "Missing \"" + expected + "\" in:\n" + output());
    }

    /** Closes standard input, the examples' stop signal. */
    void stop() throws IOException {
        process.getOutputStream().close();
    }

    int await(Duration timeout) throws InterruptedException {
        assertTrue(process.waitFor(timeout.toNanos(), TimeUnit.NANOSECONDS), "Example did not exit:\n" + output());
        return process.exitValue();
    }

    @Override
    public void close() throws Exception {
        if (process.isAlive()) {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
        }
        Files.deleteIfExists(output);
    }
}
