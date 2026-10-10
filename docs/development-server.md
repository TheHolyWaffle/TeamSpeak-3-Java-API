# Local development server

With JDK 25 selected in `JAVA_HOME`, the Maven Wrapper and a running Docker
daemon, start TS3 and run a compiled Java example in one command:

```sh
./mvnw -ntp -Pdev-server test-compile exec:exec
```

For TS6, select its profile:

```sh
./mvnw -ntp -Pdev-server test-compile exec:exec -Ddev.server=ts6
```

`dev.server` accepts only `ts3` (default) or `ts6`. Each command compiles the
library, harness and [example](../src/test/java/com/github/theholywaffle/teamspeak3/DevelopmentServerExample.java),
starts a fresh server, waits for authenticated ServerQuery readiness and runs the
example over SSH. The example selects virtual server 1, reads the version and
creates, lists and deletes a channel. An example or startup failure fails the
command and cleans up the instance. Docker is required; failures are never skips.

The launcher prints the exact image, container ID, host, mapped SSH query port,
TS3 raw query port and disposable serveradmin login. Use those details from
another terminal to test query clients while the launcher remains open. Ports
are allocated dynamically, so multiple launchers and integration tests can run
independently. This launcher exposes ServerQuery only; it does not expose voice
or file transfer ports. TS6 raw query is not enabled by the fixture.

## Stop and reset

Press **Enter** or **Ctrl+C** in the launcher's terminal to stop and remove the
container. End of standard input also stops it, so keep stdin open for an
interactive session. Data is stored in container tmpfs; stopping destroys all
server changes. Start the command again to reset to a clean instance with new
mapped ports and a new SSH host key. The example pins the freshly created
container's SSH key in memory; it does not change `user.home` or write host trust files.

For a noninteractive run that exits after the example:

```sh
./mvnw -B -ntp -Pdev-server test-compile exec:exec -Ddev.server=ts6 -Ddev.once=true
```

The launcher forks a dedicated JVM with an explicit shutdown hook. Testcontainers
Ryuk is the fallback for abrupt JVM termination; leave it enabled and do not
enable container reuse. If Docker becomes unavailable during shutdown, restart
Docker and remove the printed container ID with `docker rm -f <container-id>`.
Avoid forced termination when possible because it bypasses the Java shutdown hook.

This is a disposable development session that stays alive until stopped, not a
persistent server. Integration tests still own separate, short-lived fixtures
and never connect to the launcher. No volumes, reuse, fixed ports or Compose
stack are provided. Persistent deployment needs its own reviewed configuration
and credentials; do not reuse the fixed test password or fixture allowlist.

## Images, licenses and migration

The launcher directly uses `TeamSpeakContainer`, so image digests, environment,
allowlist, tmpfs and readiness are identical to the [integration fixtures](integration-tests.md).
Read the TS3/TS6 licenses before starting: the fixture accepts them explicitly.
TS3 runs Linux amd64 and needs Docker emulation on Apple Silicon; TS6 uses the
daemon's native architecture. The pinned TS6 beta license needs periodic
revalidation as described in the integration guide.

This is internal development tooling (`chore:` release intent). There are no
production API, dependency or runtime changes and no consumer migration steps.
The launcher and example live in test sources; Testcontainers stays test-scoped
and is excluded from normal, standalone and source library artifacts. The Maven
`dev-server` profile only configures the opt-in Exec plugin; normal builds do
not start Docker. Use `test-compile` before `exec:exec` to run compiled classes,
rather than Java source-file launching or preview features.

Implementation references: [Exec forked Java programs](https://www.mojohaus.org/exec-maven-plugin/examples/example-exec-for-java-programs.html),
[Exec configuration](https://www.mojohaus.org/exec-maven-plugin/exec-mojo.html),
[Testcontainers networking](https://java.testcontainers.org/features/networking/),
[Testcontainers cleanup](https://java.testcontainers.org/features/garbage_collection/).
