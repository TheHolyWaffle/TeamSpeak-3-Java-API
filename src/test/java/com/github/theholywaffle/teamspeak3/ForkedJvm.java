package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** A main class in its own JVM on the test classpath, with only the given TS3_* variables. */
final class ForkedJvm implements AutoCloseable {
    private static final String EXAMPLES = "com.github.theholywaffle.teamspeak3.example.";

    private final Process process;
    private final Path output;

    private ForkedJvm(Process process, Path output) {
        this.process = process;
        this.output = output;
    }

    static ForkedJvm start(String mainClass, Map<String, String> environment, String... args) throws IOException {
        Path output = Files.createTempFile("forked-jvm-", ".log");
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                System.getProperty("java.class.path"),
                mainClass));
        command.addAll(List.of(args));
        ProcessBuilder builder =
                new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().keySet().removeIf(name -> name.startsWith("TS3_"));
        builder.environment().putAll(environment);
        return new ForkedJvm(builder.start(), output);
    }

    /** Runs an example as {@code -Pexample exec:exec} does. */
    static ForkedJvm example(String example, Map<String, String> environment) throws IOException {
        return start(EXAMPLES + example, environment);
    }

    /** Runs an example with closed standard input, so long-running examples stop after their setup. */
    static String runExample(String example, Map<String, String> environment, int expectedExit) throws Exception {
        try (ForkedJvm jvm = example(example, environment)) {
            jvm.closeInput();
            int exit = jvm.await(Duration.ofSeconds(90));
            assertEquals(expectedExit, exit, jvm.output());
            return jvm.output();
        }
    }

    String output() {
        try {
            return Files.readString(output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<String> lines() {
        return output().lines().toList();
    }

    void awaitOutput(String expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!output().contains(expected) && process.isAlive() && System.nanoTime() < deadline) Thread.sleep(100);
        assertTrue(output().contains(expected), "Missing \"" + expected + "\" in:\n" + output());
    }

    void pressEnter() throws IOException {
        process.getOutputStream().write('\n');
        process.getOutputStream().flush();
    }

    void closeInput() throws IOException {
        process.getOutputStream().close();
    }

    void terminate() {
        process.destroy();
    }

    int await(Duration timeout) throws InterruptedException {
        assertTrue(process.waitFor(timeout.toNanos(), TimeUnit.NANOSECONDS), "Process did not exit:\n" + output());
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
