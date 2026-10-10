package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.wrapper.Version;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Disposable server with no host data or fixed host ports. Credentials are test-only. */
final class TeamSpeakContainer extends GenericContainer<TeamSpeakContainer> {
	static final String PASSWORD = "integration-test-only";
	static final String TS3_IMAGE = "teamspeak:3.13.8@sha256:6dfdfb22869adf50e1b66d024b360b786d57a85ea09e8e8fdb6cca23949b2813";
	static final String TS6_IMAGE = "teamspeaksystems/teamspeak6-server:6.0.0-beta13.1@sha256:d845f803aecf842c2df034c959c23ff2462357bacafb3807500885c5ec170fa1";
	final boolean ts6;
	PublicKey hostKey;
	String sshBanner;
	String algorithms;
	String queryBanner;

	TeamSpeakContainer(boolean ts6) {
		super(DockerImageName.parse(ts6 ? TS6_IMAGE : TS3_IMAGE));
		this.ts6 = ts6;
		// Only this disposable server: permit Docker's dynamically assigned bridge peers.
		withCopyToContainer(Transferable.of("0.0.0.0/0\n::/0\n"), "/tmp/query-allowlist.txt");
		withExposedPorts(ts6 ? new Integer[]{10022} : new Integer[]{10011, 10022});
		withTmpFs(Map.of(ts6 ? "/var/tsserver" : "/var/ts3server", ts6 ? "rw,uid=9987,gid=9987,mode=0700" : "rw"));
		if (ts6) {
			withEnv("TSSERVER_LICENSE_ACCEPTED", "accept");
			withEnv("TSSERVER_QUERY_SSH_ENABLED", "1");
			withEnv("TSSERVER_QUERY_ADMIN_PASSWORD", PASSWORD);
			withEnv("TSSERVER_QUERY_ALLOW_LIST", "/tmp/query-allowlist.txt");
		} else {
			withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64"));
			withEnv("TS3SERVER_LICENSE", "accept");
			withEnv("TS3SERVER_QUERY_PROTOCOLS", "raw,ssh");
			withEnv("TS3SERVER_SERVERADMIN_PASSWORD", PASSWORD);
			withEnv("TS3SERVER_IP_ALLOWLIST", "/tmp/query-allowlist.txt");
		}
		waitingFor(new AbstractWaitStrategy() {
			@Override protected void waitUntilReady() {
				long deadline = System.nanoTime() + startupTimeout.toNanos();
				Exception last = null;
				do {
					try {
						probeSsh();
						if (!ts6) probeRaw();
						return;
					} catch (Exception e) {
						last = e;
						if (!isRunning()) throw new IllegalStateException("Server container exited before readiness", e);
						try { Thread.sleep(250); } catch (InterruptedException interrupted) {
							Thread.currentThread().interrupt();
							throw new IllegalStateException("Readiness interrupted", interrupted);
						}
					}
				} while (System.nanoTime() < deadline);
				throw new IllegalStateException("Authenticated ServerQuery readiness failed for " + getDockerImageName(), last);
			}
		}.withStartupTimeout(Duration.ofSeconds(90)));
	}

	private void probeRaw() throws IOException {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(getHost(), getMappedPort(10011)), 2000);
			socket.setSoTimeout(2000);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			OutputStream out = socket.getOutputStream();
			command(in, out, "login client_login_name=serveradmin client_login_password=" + PASSWORD);
			command(in, out, "version");
			command(in, out, "use sid=1");
		}
	}

	private void probeSsh() throws IOException {
		DefaultConfig config = new DefaultConfig();
		config.setChannelReadTimeoutMs(2000);
		try (SSHClient client = new SSHClient(config)) {
			client.setConnectTimeout(2000);
			client.setTimeout(2000);
			// Bootstrap only from our freshly created, isolated container; never a user server.
			client.addHostKeyVerifier(new HostKeyVerifier() {
				@Override public boolean verify(String host, int port, PublicKey key) {
					if (hostKey == null) hostKey = key;
					return hostKey.equals(key);
				}
				@Override public List<String> findExistingAlgorithms(String host, int port) {
					return List.of();
				}
			});
			client.addAlgorithmsVerifier(negotiated -> { algorithms = negotiated.toString(); return true; });
			client.connect(getHost(), getMappedPort(10022));
			sshBanner = "SSH-2.0-" + client.getTransport().getServerVersion();
			client.authPassword("serveradmin", PASSWORD);
			// SSHClient owns the session. libssh may close the transport on channel close.
			Session session = client.startSession();
			session.startShell();
			BufferedReader in = new BufferedReader(new InputStreamReader(session.getInputStream(), StandardCharsets.UTF_8));
			queryBanner = command(in, session.getOutputStream(), "version");
			command(in, session.getOutputStream(), "use sid=1");
		}
	}

	private static String command(BufferedReader in, OutputStream out, String command) throws IOException {
		out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
		out.flush();
		StringBuilder response = new StringBuilder();
		for (String line; (line = in.readLine()) != null;) {
			if (line.startsWith("error ")) {
				if (!line.startsWith("error id=0 msg=ok")) throw new IOException("Readiness command rejected: " + line);
				return response.toString();
			}
			response.append(line).append('\n');
		}
		throw new EOFException("ServerQuery closed before command completion");
	}

	@Override public String getLogs() {
		return super.getLogs().replaceAll("(?im)^.*(?:token=|password[=:]).*$", "[redacted generated credentials]");
	}

	TS3Config config(TS3Query.Protocol protocol) {
		return new TS3Config().setHost(getHost()).setQueryPort(getMappedPort(protocol == TS3Query.Protocol.RAW ? 10011 : 10022))
				.setServerType(ts6 ? ServerType.TS6 : ServerType.TS3).setSshHostKeyPolicy(SshHostKeyPolicy.pinnedKey(hostKey))
				.setFloodRate(TS3Query.FloodRate.UNLIMITED).setProtocol(protocol).setLoginCredentials("serveradmin", PASSWORD).setCommandTimeout(5000);
	}

	void record(String name, Version version) throws IOException {
		Path directory = Path.of("target", "compatibility");
		Files.createDirectories(directory);
		Files.writeString(directory.resolve(name + ".txt"), "image=" + getDockerImageName() + "\nversion=" + version.getVersion()
				+ "\nbuild=" + version.getBuild() + "\nplatform=" + version.getPlatform() + "\ndaemonArchitecture="
				+ getDockerClient().infoCmd().exec().getArchitecture() + "\nimageArchitecture="
				+ getDockerClient().inspectImageCmd(getContainerInfo().getImageId()).exec().getArch() + "\nsshBanner=" + sshBanner
				+ "\nalgorithms=" + algorithms + "\nqueryGreetingAndVersion=" + queryBanner);
	}
}
