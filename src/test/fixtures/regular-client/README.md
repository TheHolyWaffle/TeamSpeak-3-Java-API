# Headless regular-client fixture

`./mvnw -B -ntp -Pintegration verify` builds this independent Java 25 project
through Maven Invoker, then copies its shaded JAR into a digest-pinned Java 25
container. It does not add dependencies to the library or its published POM.
Docker supplies the server alias and UDP path on a fresh private network. There
are no fixed host ports, host identities, installed clients, desktop automation,
audio devices, or manual steps. Docker must be available; failures never skip.

The fixture uses the full UDP client protocol through
[TS3j](https://github.com/Manevolent/ts3j/tree/db57d60c989e399626aa16d921390f5033e6cdeb),
pinned to that commit. Each process generates a disposable identity in memory.
It supplies no microphone/audio source. A regular client (`client_type=0`) proves
client administration and notifications, not audio or official desktop-client
feature compatibility. ServerQuery observers still use this project's library.

Only readiness, connect, disconnect and a small command allowlist are exposed.
Mutation requests require a random per-container token. Commands are bounded to
8,192 bytes and ten seconds; a timeout closes and permanently fails the fixture
session. Host HTTP deadlines exceed client deadlines, including the dependency's
30-second disconnect wait. The container is destroyed after each test target.
Evidence records field names and assertions, not identities, tokens or passwords.

The isolated dependency set replaces TS3j's old Bouncy Castle and DNSJava pins
with 1.86 and 3.6.5, respectively, and uses SLF4J 2.0.20. Commons Lang 2.6 and
ini4j 0.5.1 remain upstream implementation dependencies in this test fixture only.
JitPack resolves the immutable TS3j commit; it is not a runtime library repository.

Two Apache-2.0 source overlays preserve compatibility with the current crypto
provider. Their upstream source and license are retained here:

- `PacketTransformation`: separate receive/send cipher state; identical consecutive
  UDP retransmissions return cached ciphertext and restore the packet MAC. Changed
  plaintext with the same authenticated-traffic key/nonce still fails through the
  provider's nonce-reuse guard. The cache holds one packet and copies returned data.
- `InitPacketTransformation`: the TS3 pre-authentication handshake uses a public,
  protocol-defined constant key/nonce for different handshake packets. Those packets
  use individual cipher instances. Authenticated traffic uses the guarded base class.

`PacketTransformationTest` exercises retransmission identity/MAC, returned-buffer
ownership, changed-plaintext rejection, separate receive/send state and the fixed
public handshake. The overlays are fixture infrastructure, not changes to the
ServerQuery library or a general-purpose crypto recommendation.

Official tool guidance: [Maven Invoker](https://maven.apache.org/plugins/maven-invoker-plugin/run-mojo.html),
[TS3j connection setup](https://github.com/Manevolent/ts3j/blob/db57d60c989e399626aa16d921390f5033e6cdeb/README.md),
[Bouncy Castle EAX implementation](https://github.com/bcgit/bc-java/blob/r1rv86/core/src/main/java/org/bouncycastle/crypto/modes/EAXBlockCipher.java).
