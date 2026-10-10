package com.github.theholywaffle.teamspeak3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Development-only entry point; never included in library artifacts. */
public final class DevelopmentServer {
	private DevelopmentServer() {}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 || !(args[0].equals("ts3") || args[0].equals("ts6"))
				|| !(args[1].equals("true") || args[1].equals("false"))) {
			throw new IllegalArgumentException("Use -Ddev.server=ts3|ts6 and -Ddev.once=true|false");
		}
		Path home = Files.createTempDirectory("teamspeak-dev-");
		try (Resources resources = new Resources(new TeamSpeakContainer(args[0].equals("ts6")), home)) {
			Thread hook = new Thread(resources::close, "teamspeak-dev-cleanup");
			Runtime.getRuntime().addShutdownHook(hook);
			try {
				TeamSpeakContainer server = resources.server;
				server.start();
				System.out.println("Development server ready: " + args[0] + " container=" + server.getContainerId());
				System.out.println("Image: " + server.getDockerImageName());
				System.out.println("Disposable workspace: " + home);
				System.out.println("SSH ServerQuery: " + server.getHost() + ":" + server.getMappedPort(10022));
				if (!server.ts6) System.out.println("Raw ServerQuery: " + server.getHost() + ":" + server.getMappedPort(10011));
				System.out.println("Disposable login: serveradmin / " + TeamSpeakContainer.PASSWORD + "; virtual server: 1");
				DevelopmentServerExample.run(server.config(TS3Query.Protocol.SSH));
				if (!Boolean.parseBoolean(args[1])) {
					System.out.println("Press Enter or Ctrl+C to stop and remove the server. EOF also stops it.");
					new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
				}
			} finally {
				// Keep the hook installed until cleanup completes: SIGTERM can also close stdin.
				try { resources.close(); } finally {
					try { Runtime.getRuntime().removeShutdownHook(hook); }
					catch (IllegalStateException shuttingDown) { /* Shutdown is already running the hook. */ }
				}
			}
		}
	}

	private static final class Resources implements AutoCloseable {
		final TeamSpeakContainer server;
		final Path home;
		boolean closed;

		Resources(TeamSpeakContainer server, Path home) {
			this.server = server;
			this.home = home;
		}

		@Override public synchronized void close() {
			if (closed) return;
			closed = true;
			try {
				server.close();
			} finally {
				try (var paths = Files.walk(home)) {
					for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
				} catch (Exception e) {
					throw new IllegalStateException("Could not remove disposable SSH home " + home, e);
				}
			}
			System.out.println("Development server removed; disposable data and SSH trust reset.");
		}
	}
}
