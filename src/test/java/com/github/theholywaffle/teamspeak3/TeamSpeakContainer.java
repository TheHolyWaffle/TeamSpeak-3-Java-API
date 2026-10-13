package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.theholywaffle.teamspeak3.api.wrapper.Version;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * Disposable server with no host data or fixed host ports. Credentials are test-only.
 * Register as a static {@code @RegisterExtension} field to share one server across a test class.
 */
final class TeamSpeakContainer extends GenericContainer<TeamSpeakContainer>
        implements BeforeAllCallback, AfterAllCallback {
    static final String PASSWORD = "integration-test-only";
    private static final String ALLOWLIST_LOADED = "updated query_ip_allowlist ips: 0.0.0.0/0";
    static final String TS3_IMAGE =
            "teamspeak:3.13.8@sha256:6dfdfb22869adf50e1b66d024b360b786d57a85ea09e8e8fdb6cca23949b2813";
    PublicKey hostKey;
    String sshBanner;
    String algorithms;
    String queryBanner;

    TeamSpeakContainer() {
        super(DockerImageName.parse(TS3_IMAGE));
        // Only this disposable server: permit Docker's dynamically assigned bridge peers.
        withCopyToContainer(Transferable.of("0.0.0.0/0\n::/0\n"), "/tmp/query-allowlist.txt");
        withExposedPorts(10011, 10022);
        withTmpFs(Map.of("/var/ts3server", "rw"));
        withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64"));
        withEnv("TS3SERVER_LICENSE", "accept");
        withEnv("TS3SERVER_QUERY_PROTOCOLS", "raw,ssh");
        withEnv("TS3SERVER_SERVERADMIN_PASSWORD", PASSWORD);
        withEnv("TS3SERVER_IP_ALLOWLIST", "/tmp/query-allowlist.txt");
        waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        long deadline = System.nanoTime() + startupTimeout.toNanos();
                        Exception last = null;
                        do {
                            try {
                                // Connections before the allowlist is active count against flood protection
                                // and can block later queries from the Docker gateway address.
                                if (!getLogs().contains(ALLOWLIST_LOADED))
                                    throw new IOException("Allowlist not loaded");
                                probeSsh();
                                probeRaw();
                                return;
                            } catch (Exception e) {
                                last = e;
                                if (!isRunning())
                                    throw new IllegalStateException("Server container exited before readiness", e);
                                try {
                                    Thread.sleep(250);
                                } catch (InterruptedException interrupted) {
                                    Thread.currentThread().interrupt();
                                    throw new IllegalStateException("Readiness interrupted", interrupted);
                                }
                            }
                        } while (System.nanoTime() < deadline);
                        throw new IllegalStateException(
                                "Authenticated ServerQuery readiness failed for " + getDockerImageName(), last);
                    }
                }.withStartupTimeout(Duration.ofSeconds(90)));
    }

    TeamSpeakContainer withFileTransferPort() {
        addExposedPort(30033);
        return this;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // Deliberately fail, never skip, when the integration profile cannot use Docker.
        assertTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Required Docker integration tests cannot run: start a Docker daemon");
        start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        close();
    }

    private void probeRaw() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(getHost(), getMappedPort(10011)), 2000);
            socket.setSoTimeout(2000);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
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
                @Override
                public boolean verify(String host, int port, PublicKey key) {
                    if (hostKey == null) hostKey = key;
                    return hostKey.equals(key);
                }

                @Override
                public List<String> findExistingAlgorithms(String host, int port) {
                    return List.of();
                }
            });
            client.addAlgorithmsVerifier(negotiated -> {
                algorithms = negotiated.toString();
                return true;
            });
            client.connect(getHost(), getMappedPort(10022));
            sshBanner = "SSH-2.0-" + client.getTransport().getServerVersion();
            client.authPassword("serveradmin", PASSWORD);
            // SSHClient owns the session. libssh may close the transport on channel close.
            Session session = client.startSession();
            session.startShell();
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(session.getInputStream(), StandardCharsets.UTF_8));
            queryBanner = command(in, session.getOutputStream(), "version");
            command(in, session.getOutputStream(), "use sid=1");
        }
    }

    private static String command(BufferedReader in, OutputStream out, String command) throws IOException {
        out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        StringBuilder response = new StringBuilder();
        for (String line; (line = in.readLine()) != null; ) {
            if (line.startsWith("error ")) {
                if (!line.startsWith("error id=0 msg=ok")) throw new IOException("Readiness command rejected: " + line);
                return response.toString();
            }
            response.append(line).append('\n');
        }
        throw new EOFException("ServerQuery closed before command completion");
    }

    @Override
    public String getLogs() {
        return super.getLogs()
                .replaceAll("(?im)^.*(?:token=|password[=:]|apikey=).*$", "[redacted generated credentials]");
    }

    TS3Config config(TS3Query.Protocol protocol) {
        return new TS3Config()
                .setHost(getHost())
                .setQueryPort(getMappedPort(protocol == TS3Query.Protocol.RAW ? 10011 : 10022))
                .setSshHostKeyPolicy(SshHostKeyPolicy.pinnedKey(hostKey))
                .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                .setProtocol(protocol)
                .setLoginCredentials("serveradmin", PASSWORD)
                .setCommandResponseTimeout(Duration.ofSeconds(5));
    }

    /** Writes an OpenSSH trust file holding only this container's SSH endpoint and key. */
    Path writeKnownHosts(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        String endpoint = "[" + getHost() + "]:" + getMappedPort(10022);
        String line = new OpenSSHKnownHosts.HostEntry(null, endpoint, KeyType.fromKey(hostKey), hostKey).getLine();
        Files.writeString(file, line + "\n", StandardCharsets.UTF_8);
        return file;
    }

    /** @return the {@code TS3_*} variables the examples need to reach this container */
    Map<String, String> exampleEnvironment(TS3Query.Protocol protocol, Path knownHosts) {
        var environment = new LinkedHashMap<String, String>();
        environment.put("TS3_HOST", getHost());
        environment.put("TS3_USERNAME", "serveradmin");
        environment.put("TS3_PASSWORD", PASSWORD);
        environment.put("TS3_FLOOD_RATE", "unlimited");
        environment.put("TS3_PROTOCOL", protocol.name().toLowerCase(Locale.ROOT));
        environment.put(
                "TS3_QUERY_PORT", Integer.toString(getMappedPort(protocol == TS3Query.Protocol.RAW ? 10011 : 10022)));
        if (protocol == TS3Query.Protocol.SSH) environment.put("TS3_SSH_KNOWN_HOSTS", knownHosts.toString());
        if (getExposedPorts().contains(30033)) {
            environment.put("TS3_FILE_TRANSFER_HOST", getHost());
            environment.put("TS3_FILE_TRANSFER_PORT", Integer.toString(getMappedPort(30033)));
        }
        return environment;
    }

    void record(String name, Version version) throws IOException {
        Path directory = Path.of("target", "compatibility");
        Files.createDirectories(directory);
        Files.writeString(
                directory.resolve(name + ".txt"),
                "image=" + getDockerImageName() + "\nversion=" + version.version()
                        + "\nbuild=" + version.build() + "\nplatform=" + version.platform() + "\ndaemonArchitecture="
                        + getDockerClient().infoCmd().exec().getArchitecture() + "\nimageArchitecture="
                        + getDockerClient()
                                .inspectImageCmd(getContainerInfo().getImageId())
                                .exec()
                                .getArch() + "\nsshBanner=" + sshBanner
                        + "\nalgorithms=" + algorithms + "\nqueryGreetingAndVersion=" + queryBanner);
    }
}
