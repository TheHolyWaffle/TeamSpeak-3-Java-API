# Local development server

With JDK 25 selected in `JAVA_HOME`, the Maven Wrapper and a running Docker
daemon, start TS3 and run a compiled Java example in one command:

```sh
./mvnw -ntp -Pdev-server test-compile exec:exec
```

`dev.server` accepts only `ts3` (default). Each command compiles the
library, harness and [example](../src/test/java/com/github/theholywaffle/teamspeak3/DevelopmentServerExample.java),
starts a fresh server, waits for authenticated ServerQuery readiness and runs the
example over SSH. The example selects virtual server 1, reads the version and
creates, lists and deletes a channel. An example or startup failure fails the
command and cleans up the instance. Docker is required; failures are never skips.

The launcher prints the exact image, container ID, host, mapped SSH query port,
TS3 raw query port, file transfer port, Web Query HTTP and HTTPS endpoints and
disposable serveradmin login, followed by `export` lines for the
[examples](../README.md#run-an-example):

```text
Run an example from another terminal in this directory (POSIX shell):
  export TS3_HOST='127.0.0.1' TS3_USERNAME='serveradmin' TS3_PASSWORD='…' TS3_FLOOD_RATE='unlimited' TS3_PROTOCOL='raw' TS3_QUERY_PORT='…' TS3_FILE_TRANSFER_HOST='127.0.0.1' TS3_FILE_TRANSFER_PORT='…'
  ./mvnw -q -ntp -Pexample test-compile exec:exec -Dexample=ClientInfoExample
To use SSH instead of raw TCP, also run:
  export TS3_PROTOCOL='ssh' TS3_QUERY_PORT='…' TS3_SSH_KNOWN_HOSTS='…/target/dev-server/<container-id>/known_hosts'
For WebQueryExample (HTTPS, a disposable API key and this server's certificate), run:
  export TS3_WEBQUERY_URI='https://127.0.0.1:…' TS3_API_KEY='…' TS3_WEBQUERY_CERTIFICATE='…/target/dev-server/<container-id>/webquery-certificate.pem'
```

Paste them into another terminal to run any example, or use the printed ports with
other query clients while the launcher remains open. Ports are allocated
dynamically, so multiple launchers and integration tests can run independently.
The file transfer port is mapped and the examples map the server's advertised file
endpoint to it. Voice ports are not exposed. `DevelopmentServerIT` follows these
printed instructions, running an example over raw TCP and SSH and `WebQueryExample`
over HTTPS, for every stop mode.

## Stop and reset

Press **Enter** or **Ctrl+C** in the launcher's terminal to stop and remove the
container. End of standard input also stops it, so keep stdin open for an
interactive session. Data is stored in container tmpfs; stopping destroys all
server changes. Start the command again to reset to a clean instance with new
mapped ports and a new SSH host key. The launcher's own example pins the freshly
created container's SSH key in memory. For other terminals it writes only that key to
`target/dev-server/<container-id>/known_hosts` and deletes the file when it stops; it
never changes `user.home` or `~/.ssh/known_hosts`. Web Query HTTPS uses a certificate
generated for that container only, written next to it as `webquery-certificate.pem`
and deleted the same way; the printed API key is an unlimited instance key of the
disposable server and disappears with it.

For a noninteractive run that exits after the example:

```sh
./mvnw -B -ntp -Pdev-server test-compile exec:exec -Ddev.once=true
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
Read the TS3 license before starting: the fixture accepts it explicitly.
TS3 runs Linux amd64 and needs Docker emulation on Apple Silicon.
The former `-Ddev.server=ts6` option is removed and rejected before Docker starts.

This is internal development tooling. There are no production API, dependency or
runtime changes and no consumer migration steps. The launcher lives in test sources
and the examples in `src/example/java`, compiled as test sources; Testcontainers stays test-scoped
and is excluded from the normal and source library artifacts. The Maven
`dev-server` profile only configures the opt-in Exec plugin; normal builds do
not start Docker. Use `test-compile` before `exec:exec` to run compiled classes,
rather than Java source-file launching or preview features.

Implementation references: [Exec forked Java programs](https://www.mojohaus.org/exec-maven-plugin/examples/example-exec-for-java-programs.html),
[Exec configuration](https://www.mojohaus.org/exec-maven-plugin/exec-mojo.html),
[Testcontainers networking](https://java.testcontainers.org/features/networking/),
[Testcontainers cleanup](https://java.testcontainers.org/features/garbage_collection/).
