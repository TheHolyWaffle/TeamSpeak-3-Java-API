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
[local development launcher](development-server.md). Its compiled example,
printed example instructions and Enter/EOF/termination cleanup are also exercised
by this integration profile; each launcher owns a separate disposable server.

`ExamplesIT` runs every class in `src/example/java` unchanged, in its own JVM,
over TS3 raw and TS3 SSH, configured only through the `TS3_*` variables
documented in the [README](../README.md#run-an-example). A new example must
declare its expected completion output there or the test fails. The test also
exchanges `!ping`/`pong` with the running chat bot and checks that a missing
setting fails before connecting.

`TeamSpeakContainer` uses the official TS3 image pinned by version and manifest digest in
[`src/test/resources/teamspeak3/Dockerfile`](../src/test/resources/teamspeak3/Dockerfile), the only
place the image is named. Its tag is the server version the suite expects, and the README support
table must name it. Dependabot maintains the pin; see
[dependency and image maintenance](maintenance.md).

TS3 explicitly requests `linux/amd64`: Apple Silicon needs Docker amd64
emulation. This is not native TS3 arm64 support. The Linux CI suite uses amd64; local Apple Silicon validation is
recorded in the PR acceptance evidence. A manifest alone is not a runtime test.

The fixture explicitly accepts the TS3 license (`TS3SERVER_LICENSE=accept`).
Read the license before running the profile. The bundled limited TS3 license suffices
for one virtual server; no private license or publishing credentials are used.
The test-only serveradmin password is fixed, ports are assigned dynamically, and
data lives in container tmpfs. A fixture-only allowlist exempts Docker bridge
addresses from query flood protection; never reuse this configuration for a
persistent or publicly accessible server. Do not enable reuse or disable Ryuk.
Try/finally cleanup stops servers after failures; Ryuk handles JVM termination.

Readiness first waits for the server to log that the fixture allowlist is active;
TS3 starts accepting queries a few seconds earlier, and connections in that window
count against flood protection, which intermittently blocked later queries from
the Docker gateway address. It then repeatedly authenticates and successfully
executes `version` and `use sid=1` over SSH, plus login/version/selection over TS3 raw. The retry delay
backs off failed probes; an open port or an unconditional startup sleep does not
establish readiness. SSH host trust is bootstrapped only from the freshly created
fixture. Normal library connections pin that key in memory. TOFU security tests
use an explicit JUnit temporary trust file. Neither path changes `user.home`
or writes to the developer's home directory.

The same parameterized API smoke test covers TS3 raw and TS3 SSH:
initial authenticated connection, exact server version, virtual server selection,
channel create/list/delete (including protocol escapes), typed server errors and
usable framing after an error. Additional live SSH regressions cover first-key
persistence, subsequent trusted connection, rejected credentials and a changed
RSA host key. The readiness client records server SSH banners, negotiated crypto
and the query greeting before any transport/API redesign. Reports under
`target/compatibility/` and Failsafe reports record exact server version/build,
image digest and daemon architecture. CI uploads them even on failure. Generated
server credentials (passwords, privilege keys and API keys) are redacted from
container startup diagnostics.

## CI and scheduled revalidation

The required Maven CI calls `compatibility.yml` on Linux with Java 25, and the
aggregate `CI required` job requires its success. The workflow also supports
manual runs, optionally against a candidate image, and weekly scheduled revalidation
of the pin. **Upstream update detection** runs the same suite against a newer official
TS3 image as soon as one is published; see [scheduled detection](maintenance.md#scheduled-detection).
GitHub runs scheduled and manually dispatched workflows only when they exist on the
default branch, so both become active after final integration into `master`. No workflow
here publishes artifacts or changes the pin; Dependabot proposes pin updates as PRs.

Review a newer official TS3 version/digest, inspect its manifest, rerun this suite
on Linux amd64 and Apple Silicon, update the expected version and pins (including
the CI manifest probe), and review negotiation evidence before accepting an update.
Scheduled failures require investigation, not skipped tests.

## Migration and scope

This is test/build infrastructure (`test:` release intent), with no production API
or default changes. Testcontainers is test-scoped and is excluded from published
runtime dependencies and the published artifacts. The integration profile is
opt-in locally and mandatory in modernization CI. This baseline verifies only
the exact two TS3 combinations above; it does not promise untested commands,
notifications, file transfers or voice. Broader command/event coverage
and transport redesign remain with their later owning tasks.

Sources: [official TS3 image documentation](https://hub.docker.com/_/teamspeak),
[TS3 image entrypoint](https://github.com/TeamSpeak-Systems/teamspeak-linux-docker-images/blob/e03f9a91761c389d42568b7c0cdee63e77d38fb0/alpine/entrypoint.sh),
[Testcontainers waits](https://java.testcontainers.org/features/startup_and_waits/),
[Testcontainers 2.0.5 release](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.5),
[FailSafe usage](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).

The task #438 notification probe additionally registers overlapping server/channel
subscriptions and checks query-client join, move and leave frames on TS3 RAW,
and TS3 SSH. It records decoded payloads in
`target/compatibility/*-notifications.txt` and asserts one frame per tested action,
no malformed notifications, callback failures or event loss. Scripted tests cover
unknown/malformed notifications and repeated identical frames that real servers
are not expected to emit on demand. See [event migration notes](events-and-capacity.md).

Task #441 expands the baseline with independent-session channel domain tests,
live command help, decoded field inventories and typed channel-error checks. See
the [command/event inventory and migration notes](command-compatibility.md) for
the exact supported subset and remaining domains.

TS6 server selection, fixtures, image pins, launcher options and CI coverage have
been removed from this release scope. Historical PR evidence remains historical.

## Web Query

`WebQueryCompatibilityIT` uses `TeamSpeakContainer.withWebQuery()`, which also enables
`http,https` and exposes ports 10080 and 10443. HTTPS uses a key pair and self-signed
certificate generated with Bouncy Castle for each fixture, valid for one day for
`localhost`, `127.0.0.1` and the Docker host. Only the test client trusts it, through an
in-memory `SSLContext`; the server reads the PEM files copied into the container. Readiness
additionally waits for both Web Query listeners. API keys are created over RAW for that
fixture only and redacted from container logs and evidence. Each domain runs over HTTP and
HTTPS against the same pinned image. Lifecycle, snapshots, permission reset and process
shutdown use a second disposable server. Evidence goes to `target/compatibility/TS3_WEBQUERY*`.
Coverage and limits are listed in [Web Query](web-query.md#supported-operations).
