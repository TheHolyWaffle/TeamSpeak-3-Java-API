# ServerQuery integration tests

Use JDK 25, the Maven Wrapper, and a running Docker daemon. No existing TeamSpeak
server, persistent data, query account or SSH trust file is needed:

```sh
./mvnw -B -ntp -Pintegration verify
```

The default `./mvnw -B -ntp verify` runs fast Surefire tests without starting
Docker. The integration profile adds Failsafe `*IT` tests; use `verify`, not just
`integration-test`, so failures fail the build. Docker absence, image pull or
license failures, failed readiness and missing integration tests fail this profile;
none are treated as skipped compatibility checks. Failsafe bounds the entire
fork to ten minutes, and each fixture allows ninety seconds for authenticated
readiness. First image pulls also need registry access.

For an interactive server session using the same fixture, see the
[local development launcher](development-server.md). Its compiled example and
Enter/EOF/termination cleanup are also exercised by this integration profile;
each launcher owns a separate disposable server.

`TeamSpeakContainer` uses official images pinned by version and manifest digest:

| Server | Image version | Manifest digest | Advertised platforms |
| --- | --- | --- | --- |
| TS3 | `teamspeak:3.13.8` | `sha256:6dfdfb22869adf50e1b66d024b360b786d57a85ea09e8e8fdb6cca23949b2813` | Linux amd64 |
| TS6 | `teamspeaksystems/teamspeak6-server:6.0.0-beta13.1` | `sha256:d845f803aecf842c2df034c959c23ff2462357bacafb3807500885c5ec170fa1` | Linux amd64, arm64 |

TS3 explicitly requests `linux/amd64`: Apple Silicon needs Docker amd64
emulation. This is not native TS3 arm64 support. TS6 uses the daemon's native
architecture. The Linux CI suite uses amd64; local Apple Silicon validation is
recorded in the PR acceptance evidence. A manifest alone is not a runtime test.

The fixture explicitly accepts each server's license (`TS3SERVER_LICENSE=accept`
and `TSSERVER_LICENSE_ACCEPTED=accept`). Read the respective licenses before
running the profile. The bundled limited TS3 license and TS6 beta license suffice
for one virtual server; no private license or publishing credentials are used.
The test-only serveradmin password is fixed, ports are assigned dynamically, and
data lives in container tmpfs. A fixture-only allowlist exempts Docker bridge
addresses from query flood protection; never reuse this configuration for a
persistent or publicly accessible server. Do not enable reuse or disable Ryuk.
Try/finally cleanup stops servers after failures; Ryuk handles JVM termination.

Readiness repeatedly authenticates and successfully executes `version` and
`use sid=1` over SSH, plus login/version/selection over TS3 raw. The retry delay
backs off failed probes; an open port or an unconditional startup sleep does not
establish readiness. SSH host trust is bootstrapped only from the freshly created
fixture. The library tests redirect `user.home` into JUnit's temporary directory
and restore it afterward, so the user's `known_ts3_hosts` is untouched. These
tests must remain sequential while this process-wide property is changed.

The same parameterized API smoke test covers TS3 raw, TS3 SSH and TS6 SSH:
initial authenticated connection, exact server version, virtual server selection,
channel create/list/delete (including protocol escapes), typed server errors and
usable framing after an error. Additional live SSH regressions cover first-key
persistence, subsequent trusted connection, rejected credentials and a changed
RSA host key. The readiness client records server SSH banners, negotiated crypto
and the query greeting before any transport/API redesign. Reports under
`target/compatibility/` and Failsafe reports record exact server version/build,
image digest and daemon architecture. CI uploads them even on failure. Generated
server credentials are redacted from container startup diagnostics.

## CI and beta revalidation

The required Maven CI calls `compatibility.yml` on Linux with Java 25, and the
aggregate `CI required` job requires its success. The workflow also supports
manual runs and weekly scheduled revalidation of the pinned images. GitHub runs
cron only for workflows present on the default branch: while modernization stays
on `modernization/2.0`, manually run **ServerQuery compatibility** on that branch
weekly; cron becomes active after final integration into `master`. No workflow
here publishes artifacts or upgrades the pin silently.

TS6 is a beta. Its upstream README says the bundled beta license is refreshed
every two months. A pinned image can therefore stop being usable without any
code change. Review a newer official version/digest, inspect its manifest, rerun
this suite on Linux amd64 and Apple Silicon, update the expected version and pins
(including the CI manifest probes), and review the negotiation evidence before
accepting an update. Scheduled failures require investigation, not skipped tests.

## Migration and scope

This is test/build infrastructure (`test:` release intent), with no production API
or default changes. Testcontainers is test-scoped and is excluded from published
runtime dependencies and the standalone artifact. The integration profile is
opt-in locally and mandatory in modernization CI. This baseline verifies only
the exact three combinations above; it does not promise all TS6 commands,
notifications, file transfers, voice or Web Query. Broader command/event coverage
and transport redesign remain with their later owning tasks.

Sources: [official TS3 image documentation](https://hub.docker.com/_/teamspeak),
[TS3 image entrypoint](https://github.com/TeamSpeak-Systems/teamspeak-linux-docker-images/blob/e03f9a91761c389d42568b7c0cdee63e77d38fb0/alpine/entrypoint.sh),
[official TS6 README](https://github.com/teamspeak/teamspeak6-server),
[TS6 configuration](https://github.com/teamspeak/teamspeak6-server/blob/main/CONFIG.md),
[Testcontainers waits](https://java.testcontainers.org/features/startup_and_waits/),
[Testcontainers 2.0.5 release](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.5),
[FailSafe usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).

The task #438 notification probe additionally registers overlapping server/channel
subscriptions and checks query-client join, move and leave frames on TS3 RAW,
TS3 SSH and TS6 SSH. It records decoded payloads in
`target/compatibility/*-notifications.txt` and asserts one frame per tested action,
no malformed notifications, callback failures or event loss. Scripted tests cover
unknown/malformed notifications and repeated identical frames that real servers
are not expected to emit on demand. See [event migration notes](events-and-capacity.md).
