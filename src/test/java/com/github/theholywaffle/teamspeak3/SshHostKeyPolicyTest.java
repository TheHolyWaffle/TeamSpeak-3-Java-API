package com.github.theholywaffle.teamspeak3;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class SshHostKeyPolicyTest {
	@TempDir Path directory;
	private static PublicKey key(String algorithm) throws Exception {
		var generator = KeyPairGenerator.getInstance(algorithm);
		if (algorithm.equals("RSA")) generator.initialize(2048);
		return generator.generateKeyPair().getPublic();
	}

	@Test void strictUnknownTrustNeverWritesAndTrustedKeyRejectsChanges() throws Exception {
		Path file = directory.resolve("known_hosts");
		PublicKey trusted = key("RSA"), changed = key("RSA");
		assertFalse(SshHostKeyPolicy.knownHosts(file).verifier().verify("localhost", 10022, trusted));
		assertFalse(Files.exists(file));
		var known = new OpenSSHKnownHosts(file.toFile());
		known.entries().add(new OpenSSHKnownHosts.HostEntry(null, "[localhost]:10022", KeyType.RSA, trusted));
		known.write();
		byte[] before = Files.readAllBytes(file);
		var verifier = SshHostKeyPolicy.knownHosts(file).verifier();
		assertTrue(verifier.verify("localhost", 10022, trusted));
		assertFalse(verifier.verify("localhost", 10022, changed));
		assertFalse(verifier.verify("localhost", 10023, trusted));
		assertArrayEquals(before, Files.readAllBytes(file));
	}

	@Test void pinnedKeyNeverNeedsATrustFileAndRejectsOtherKeys() throws Exception {
		PublicKey trusted = key("RSA");
		var verifier = SshHostKeyPolicy.pinnedKey(trusted).verifier();
		assertTrue(verifier.verify("localhost", 10022, trusted));
		assertFalse(verifier.verify("localhost", 10022, key("RSA")));
		assertFalse(verifier.verify("localhost", 10022, key("Ed25519")));
		try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
	}

	@Test void explicitTofuPersistsOnceAndRejectsChangedKeysAcrossAlgorithms() throws Exception {
		String home = System.getProperty("user.home");
		Path file = directory.resolve("tofu_hosts");
		PublicKey trusted = key("RSA");
		var policy = SshHostKeyPolicy.trustOnFirstUse(file);
		assertTrue(policy.verifier().verify("localhost", 10022, trusted));
		byte[] before = Files.readAllBytes(file);
		assertTrue(policy.verifier().verify("localhost", 10022, trusted));
		assertFalse(policy.verifier().verify("localhost", 10022, key("RSA")));
		assertFalse(policy.verifier().verify("localhost", 10022, key("Ed25519")));
		assertArrayEquals(before, Files.readAllBytes(file));
		assertFalse(SshHostKeyPolicy.knownHosts(file).verifier().verify("localhost", 10023, trusted));
		assertEquals(home, System.getProperty("user.home"));
	}

	@Test void tofuFailsClosedWhenTrustCannotBePersisted() throws Exception {
		var verifier = SshHostKeyPolicy.trustOnFirstUse(directory.resolve("missing/known_hosts")).verifier();
		assertThrows(IllegalStateException.class, () -> verifier.verify("localhost", 10022, key("RSA")));
	}
}
