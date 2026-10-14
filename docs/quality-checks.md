# Formatting, analysis, coverage and API compatibility

Every check runs in the normal Gradle build, so a local `./gradlew build` on JDK 25 runs the
same checks as the CI **Build** job.

| Check | Tool | Local command | CI job |
| --- | --- | --- | --- |
| Formatting | Spotless with Palantir Java Format, `.editorconfig` | `./gradlew spotlessApply`, checked by `build` | Build |
| Compiler and bug patterns | `javac -Xlint:all -Werror`, Error Prone | `build` | Build |
| Javadoc | `-Xdoclint` for protected API, `javadoc` with `-Xwerror` | `build` | Build |
| Coverage | JaCoCo per-class floors | `build`; `integrationTest` adds the TS3 suite | Build, ServerQuery compatibility (reports) |
| Log redaction | `CommunicationLogRedactionTest` in its own DEBUG-logging test task | `build` | Build |
| Dependency vulnerabilities | GitHub dependency graph, Dependabot alerts, dependency review | (GitHub only) | Dependency graph and review |
| API compatibility | japicmp | `build` (`./gradlew japicmp`) | Build |

Tool versions are pinned in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and kept
current by Renovate; see
[dependency and image maintenance](maintenance.md).

## Formatting

`.editorconfig` sets UTF-8, LF, final newlines and indentation for every file type;
`.gitattributes` normalizes line endings to LF (CRLF only for `gradlew.bat`). Java sources in
`src/main`, `src/test`, `src/test/consumers` and `src/example` use Palantir Java Format: 4-space
indentation and 120 columns, with imports ordered and unused imports removed. It does not
reformat comments, so license notices are unchanged. The format of YAML, shell, JSON,
properties, Gradle and consumer build files is checked for trailing whitespace and final newlines.

Run `./gradlew spotlessApply` before committing; `build` fails on unformatted files. The
formatter parses with the running JDK's compiler and supports no JDK 26+ compiler internals yet,
which is why the build requires JDK 25.

## Static analysis and Javadoc

Main and test sources compile with `-Xlint:all -Werror` and Error Prone, so every warning
fails the build. Disabled globally, with reasons:

- Error Prone `MissingSummary` and doclint `missing`: many members are fully described by
  `@return` or `@see`, so the build requires valid Javadoc, not Javadoc on every member.
- Test sources only: lint `try` (test fixtures close servers whose `close()` may be
  interrupted) and Error Prone `FutureReturnValueIgnored` (tests start futures only to observe
  their side effects).

There is no baseline or suppression file. A new finding fails the build. Fix it, or suppress
that one check at the smallest scope with a comment that says why the code is intentional.
The current suppressions mark identity comparisons, `getClass()`-based model equality and
`whenComplete` listeners. Doclint validates syntax, HTML and references in public and
protected Javadoc at compile time, and the `javadoc` tool, which builds the Javadoc JAR in
`build`, fails on any warning with the same policy.

## Coverage

`build` writes `build/reports/jacoco/test/`, from the unit and DEBUG-logging test tasks.
`integrationTest` writes `build/reports/jacoco/jacocoIntegrationTestReport/` for the Docker TS3
suite. CI uploads both.

The check is not a global percentage. Per-class floors cover the code where a regression is
hard to see in review: the protocol codec, line reader, stream reader/writer and redaction,
commands and responses, command queue, connection lifecycle, deadlines, keep-alive, events,
file transfer, futures, reconnect strategies and handlers, SSH host-key policy, the Web Query
client, configuration and protocol mapping, and core exceptions. Each class needs 80% of lines and 75% of branches. Four classes need 50% of
branches: `CommandFutures.RequestFuture`, `ReconnectingConnectionHandler`,
`ReconnectStrategy.UserControlled` and `SessionConfiguration.Subscription`. Their remaining
branches are concurrent state races, which the integration suite exercises. Domain facades
and response models are thin mappings that the TS3 RAW/SSH integration suite exercises
against a real server, so unit floors would add nothing there.

Raise a floor when coverage improves. Lowering one needs a reviewed reason in the PR.

## Redacted diagnostics

Communication logging (`TS3Config.setEnableCommunicationsLogging(true)`) logs every command,
response and notification at DEBUG. These diagnostics replace secret values with `<redacted>`:

- communication logs, and the `[UNHANDLED]` and malformed-response warnings, which are logged
  even without communication logging;
- `toString()` of `Command`, of every response model and event (`Wrapper`), and of
  `DefaultArrayResponse`.

Secrets are `*password` properties (except `*_flag_password` flags), `cpw`/`tcpw` channel
passwords, temporary passwords `pw`/`pw_clear`, privilege keys `token`, `apikey` and file
transfer keys `ftkey`, in every row of a response. In a `login` command every argument after
the user name is redacted, because a positional password may contain `=` or start with `-`.
Getters, `getRawResponse()` and `Command.encoded()`, the text sent to the server, keep the real
values. Communication log lines are only built and redacted when DEBUG is enabled. IP addresses, nicknames, unique identifiers and
messages are not redacted; review them before sharing a log.

`ProtocolRedactorTest` covers each secret property and the line structure.
`CommunicationLogRedactionTest` runs a session with DEBUG logging to a file in its own JVM and
fails if any password, key or token appears in it, or if an expected redacted line is missing.

## Dependency vulnerabilities

GitHub scans dependencies; the build runs no scanner of its own. The **Dependency graph and
review** job submits the resolved Gradle graph, including transitive and test dependencies and
versions managed by the Bouncy Castle BOM, with `gradle/actions/dependency-submission`:

- **Dependabot alerts** (enabled in the repository settings) use that graph, and Renovate opens
  the security update PRs for them.
  GitHub raises alerts for the default branch, `master`, and re-evaluates it whenever a new
  advisory is published, so no scheduled scan is needed there.
- **Dependency review** (`actions/dependency-review-action`) runs on every PR after its graph is
  submitted. It fails the PR if a dependency it adds or changes has a known vulnerability of any
  severity. PRs from forks get a read-only token and cannot submit a graph, so their review sees
  no resolved graph; review their dependency changes by hand.

Dependency review checks changed dependencies only. A new advisory against an unchanged
dependency surfaces as a Dependabot alert on `master`. Allow a vulnerability (`allow-ghsas`)
only in a reviewed PR that states why it is not exploitable here. Gradle plugins and GitHub
Actions are pinned and are not scanned here; Renovate keeps them current (see
[dependency and image maintenance](maintenance.md)).

## API compatibility

### Baseline

The baseline is the latest published release, `com.github.theholywaffle:teamspeak3-api:1.3.1`
from Maven Central. The `japicmp` task fetches it into `build/api-baseline/` and fails unless its
SHA-256 is `e76bfdb446c5d814a23ddd4f5f49486c1361fa61bcb2c8f393c7c13d96794756`. That JAR's
Central SHA-1 matches, and its signature is valid for the maintainer key
`72BF 8ED7 AA59 66B2 F5CA AA83 BEE1 8CF6 E79F 7B17`.

The artifact identity is unchanged in 2.0: groupId, artifactId, the
`com.github.theholywaffle.teamspeak3` packages and the `Automatic-Module-Name`
`com.github.theholywaffle.teamspeak3` all match 1.3.1. japicmp compares classes by name. If a
future release renames the artifact or a package, set `apiBaselineVersion` in `build.gradle.kts`
and the baseline repository in `settings.gradle.kts` explicitly. Expect every moved type to be reported as removed and added, and treat
that as a breaking change with migration notes, not an exclusion.

### What is compared

japicmp compares the public and protected API of every package, with no includes or excludes.
The report is written to `build/japicmp/api-compatibility.diff` and uploaded by the **Build** job.

The library JAR is compared with the baseline. japicmp takes each version from the file name, so
the new version is the one release-please maintains in `gradle.properties` (`2.0.0-SNAPSHOT`, then
`2.0.0` in the release PR). The build fails if that version is too small for the changes found;
publishing this API as `1.4.0`, for example, is rejected. Because the release PR carries the
version derived from the merged commits, a `fix:` that removes API fails the release PR, before
anything is published. Find the offending commit in the report and declare its impact with a
[commit override](releases.md#correct-release-notes).

| Release impact | Title | Allowed API change |
| --- | --- | --- |
| major | `type!:` or a `BREAKING CHANGE:` footer | Any |
| minor | `feat:` | Binary- and source-compatible changes, such as additions |
| patch | `fix:`, `perf:`, `deps:`, `revert:` | None |

### Intentional 2.0 breaks

The 1.3.1 comparison reports every 2.0 break, and the build accepts them only because the
pending version is a major release. Nothing is excluded. Every change since 1.3.1 stays
visible in the report artifact. Each break is reviewed when it lands and declares `!` or a
`BREAKING CHANGE:` footer with migration notes, which release-please copies into the 2.0.0 notes. The
operation-by-operation mapping is in the [domain API migration guide](api-migration.md),
and the other breaks are in the task guides linked from the README.

After 2.0.0 is published, move the baseline as described in
[after 2.0.0 is published](releases.md#after-200-is-published). From then on, the check enforces
SemVer against 2.0.0 for every release.

### Limits

japicmp checks signatures, not behavior. It cannot see:

- behavioral changes behind an unchanged signature, such as different results, exceptions,
  ordering, threading, timeouts or retry behavior;
- changed configuration defaults, such as timeouts, capacities, reconnect strategy or logging;
- Java runtime and TeamSpeak server support, protocol, transport or dependency changes,
  including a new required logging provider;
- serialization compatibility, and reflection or module access.

Each of these needs human review and the right Conventional Commit impact: `fix!:`/`feat!:`
or a `BREAKING CHANGE:` footer with migration notes for an incompatible behavior or support
change, even when japicmp reports nothing.

## Migration (2.0)

| Before | After |
| --- | --- |
| `TS3Query` could be subclassed | `TS3Query` is `final`; wrap it instead. Its constructor shares `this` with the event manager, so a subclass could observe it half-initialized |
| `TS3CommandFailedException` failed Java serialization (`QueryError` is not serializable) | It serializes, including the full error |
| Communication logs and warnings contained passwords, privilege keys and file transfer keys | Those values are `<redacted>` |
| `Wrapper.toString()` and `DefaultArrayResponse.toString()` printed secrets | Secrets are `<redacted>`; use the getters or `getRawResponse()` for real values |
| `Command.toString()` returned the wire text | `Command.encoded()` returns the wire text; `toString()` redacts secrets |

Release intent: `fix!:` for the planned **2.0.0**, with a `BREAKING CHANGE` footer listing the
rows above. Defaults, dependencies, transports and protocol are unchanged.
