package com.github.theholywaffle.teamspeak3;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

/** Immutable SSH trust policy. Unknown and changed keys are rejected by default.
 * TOFU is explicit and writes only to the supplied file; parent directories must already exist.
 */
public final class SshHostKeyPolicy {
	private enum Mode { KNOWN_HOSTS, PINNED, TOFU }
	private final Mode mode;
	private final Path file;
	private final byte[] pinned;

	private SshHostKeyPolicy(Mode mode, Path file, byte[] pinned) {
		this.mode = mode;
		this.file = file == null ? null : file.toAbsolutePath().normalize();
		this.pinned = pinned == null ? null : pinned.clone();
	}

	/** @return strict verification using the standard OpenSSH known_hosts file */
	public static SshHostKeyPolicy defaultKnownHosts() {
		return knownHosts(Path.of(System.getProperty("user.home"), ".ssh", "known_hosts"));
	}
	/** @param file OpenSSH trust file (read only) @return strict trust policy */
	public static SshHostKeyPolicy knownHosts(Path file) {
		return new SshHostKeyPolicy(Mode.KNOWN_HOSTS, Objects.requireNonNull(file), null);
	}
	/** @param key trusted server public key @return a policy comparing the encoded public key */
	public static SshHostKeyPolicy pinnedKey(PublicKey key) {
		byte[] encoded = Objects.requireNonNull(key).getEncoded();
		if (encoded == null || encoded.length == 0) throw new IllegalArgumentException("Public key must have an encoding");
		return new SshHostKeyPolicy(Mode.PINNED, null, encoded);
	}
	/** @param file application-owned OpenSSH trust file @return explicit trust on first use */
	public static SshHostKeyPolicy trustOnFirstUse(Path file) {
		return new SshHostKeyPolicy(Mode.TOFU, Objects.requireNonNull(file), null);
	}

	HostKeyVerifier verifier() throws IOException {
		if (mode == Mode.KNOWN_HOSTS) return new OpenSSHKnownHosts(file.toFile());
		return new HostKeyVerifier() {
			@Override public boolean verify(String host, int port, PublicKey key) {
				if (mode == Mode.PINNED) {
					byte[] encoded = key.getEncoded();
					return encoded != null && MessageDigest.isEqual(pinned, encoded);
				}
				// Serialize first-use decisions across queries in this JVM and reload the store.
				// Unknown algorithms for an already trusted endpoint must not bypass key rotation checks.
				synchronized (SshHostKeyPolicy.class) {
					try {
						var known = new OpenSSHKnownHosts(file.toFile());
						if (!known.findExistingAlgorithms(host, port).isEmpty()) return known.verify(host, port, key);
						KeyType type = KeyType.fromKey(key);
						if (type == KeyType.UNKNOWN) return false;
						String endpoint = host.toLowerCase(java.util.Locale.ROOT);
						if (port != 22) endpoint = "[" + endpoint + "]:" + port;
						known.entries().add(new OpenSSHKnownHosts.HostEntry(null, endpoint, type, key));
						known.write();
						return true;
					} catch (IOException failure) {
						throw new IllegalStateException("Could not persist SSH host trust", failure);
					}
				}
			}
			@Override public List<String> findExistingAlgorithms(String host, int port) { return List.of(); }
		};
	}
	@Override public String toString() { return "SshHostKeyPolicy[" + mode + "]"; }
}
