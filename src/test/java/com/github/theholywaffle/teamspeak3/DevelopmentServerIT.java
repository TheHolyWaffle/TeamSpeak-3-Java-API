package com.github.theholywaffle.teamspeak3;

import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.DockerClientFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Exercise the real forked launcher, including its JVM shutdown hook. */
class DevelopmentServerIT {
	@ParameterizedTest(name = "{0}: launcher example and {1} cleanup")
	@CsvSource({"ts3,ENTER", "ts6,EOF", "ts6,SIGNAL", "ts3,ONCE", "ts6,ONCE"})
	void exampleAndCleanup(String server, String stop) throws Exception {
		assertTrue(DockerClientFactory.instance().isDockerAvailable(), "Launcher validation requires Docker");
		Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-cp", System.getProperty("java.class.path"), DevelopmentServer.class.getName(),
				server, Boolean.toString(stop.equals("ONCE"))).redirectErrorStream(true).start();
		List<String> output = new CopyOnWriteArrayList<>();
		Thread reader = Thread.ofPlatform().start(() -> {
			try (var lines = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				for (String line; (line = lines.readLine()) != null;) output.add(line);
			} catch (Exception e) { output.add("Reader failure: " + e); }
		});
		try {
			long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
			while (process.isAlive() && output.stream().noneMatch(line -> line.startsWith("Press Enter"))
					&& !stop.equals("ONCE") && System.nanoTime() < deadline) Thread.sleep(100);
			if (!stop.equals("ONCE")) {
				assertTrue(output.stream().anyMatch(line -> line.startsWith("Press Enter")), output.toString());
				switch (stop) {
					case "ENTER" -> { process.getOutputStream().write('\n'); process.getOutputStream().flush(); }
					case "EOF" -> process.getOutputStream().close();
					case "SIGNAL" -> process.destroy();
					default -> fail("Unknown stop mode");
				}
			}
			assertTrue(process.waitFor(120, TimeUnit.SECONDS), "Launcher did not terminate: " + output);
			reader.join(5000);
			if (!stop.equals("SIGNAL")) assertEquals(0, process.exitValue(), output.toString());
			assertTrue(output.stream().anyMatch(line -> line.startsWith("Compiled example succeeded: version="
					+ (server.equals("ts3") ? "3.13.8" : "6.0.0-beta13.1"))), output.toString());
			String ready = output.stream().filter(line -> line.startsWith("Development server ready:")).findFirst().orElseThrow();
			String id = ready.substring(ready.indexOf("container=") + "container=".length());
			assertThrows(NotFoundException.class, () -> DockerClientFactory.instance().client().inspectContainerCmd(id).exec(),
					"Stopping the launcher must remove its container");
		} finally {
			if (process.isAlive()) {
				process.destroy();
				if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
			}
		}
	}
}
