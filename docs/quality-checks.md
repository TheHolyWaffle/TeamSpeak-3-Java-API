# Formatting, analysis, coverage and API compatibility

Every check runs in the normal Maven build, so a local `./mvnw -B -ntp verify` on JDK 25 runs
the same checks as the CI **Verify Java 25** job. A few checks that need extra inputs (such as a PR
title, a second checkout or a scanner binary) run from the scripts below, and CI calls the same
scripts.

| Check | Tool (pinned) | Local command | CI job |
| --- | --- | --- | --- |
| Formatting | Spotless 3.10.3 with Palantir Java Format 2.99.0, `.editorconfig` | `./mvnw spotless:apply`, checked by `verify` | Verify Java 25 |
| Compiler and bug patterns | `javac -Xlint:all -Werror`, Error Prone 2.50.0 | `verify` | Verify Java 25 and 27 |
| Javadoc | `-Xdoclint` for protected API, `javadoc` with `failOnWarnings` | `verify` | Verify Java 25 and 27 |
| Coverage | JaCoCo 0.8.15 per-class floors | `verify`; `-Pintegration verify` adds the TS3 suite | Verify (report artifact), Docker compatibility (`jacoco-it`) |
| Log redaction | `CommunicationLogRedactionTest` in its own DEBUG-logging Surefire run | `verify` | Verify Java 25 and 27 |
| Dependency vulnerabilities | OSV-Scanner 2.6.0 | `osv-scanner scan source --lockfile=pom.xml --all-vulns` | Dependency scan (also weekly) |
| API compatibility | japicmp 0.26.2 | `verify`; `bash .github/scripts/test-api-compatibility.sh` | API compatibility |

## Formatting

`.editorconfig` sets UTF-8, LF, final newlines and indentation for every file type;
`.gitattributes` normalizes line endings to LF (CRLF only for `mvnw.cmd`). Java sources in
`src/main`, `src/test`, `src/test/api-fixtures` and `example` use Palantir Java Format: 4-space
indentation and 120 columns, with imports ordered and unused imports removed. It does not
reformat comments, so license notices are unchanged. The format of YAML, shell, JSON,
properties and `pom.xml` files is checked for trailing whitespace and final newlines.

Run `./mvnw spotless:apply` before committing; `verify` fails on unformatted files. The
formatter parses with the running JDK's compiler, and no formatter release supports JDK 26+
compiler internals yet. A build on JDK 26 or newer therefore skips the format check through
the `format-check-on-baseline-jdk-only` profile. Formatting is independent of the build JDK,
and the Java 25 build and CI job enforce it.

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
`verify`, fails on any warning with the same policy.

## Coverage

`verify` writes `target/site/jacoco/` (HTML, XML and CSV). With `-Pintegration`, the Docker TS3
suite writes `target/site/jacoco-it/`. CI uploads both.

The check is not a global percentage. Per-class floors cover the code where a regression is
hard to see in review: the protocol codec, line reader, stream reader/writer and redaction,
commands and responses, command queue, connection lifecycle, deadlines, keep-alive, events,
file transfer, futures, reconnect strategies and handlers, SSH host-key policy and core
exceptions. Each class needs 80% of lines and 75% of branches. Four classes need 50% of
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
- `Wrapper.toString()` for every response model and event, and `DefaultArrayResponse.toString()`.

Secrets are `*password` properties (except `*_flag_password` flags), `cpw`/`tcpw` channel
passwords, temporary passwords `pw`/`pw_clear`, privilege keys `token`, `apikey` and file
transfer keys `ftkey`, in every row of a response. The positional password of
`login <user> <password>` is redacted too. Getters, `getRawResponse()` and the encoded command
sent to the server keep the real values. IP addresses, nicknames, unique identifiers and
messages are not redacted; review them before sharing a log.

`ProtocolRedactorTest` covers each secret property and the line structure.
`CommunicationLogRedactionTest` runs a session with DEBUG logging to a file in its own JVM and
fails if any password, key or token appears in it, or if an expected redacted line is missing.

## Dependency vulnerabilities

OSV-Scanner resolves the transitive Maven graph of `pom.xml` (compile, runtime and test
scopes), including versions managed by the Bouncy Castle BOM. It fails on any known
vulnerability. CI runs the SHA-pinned `google/osv-scanner-action` on every push and PR and
every Monday, because new advisories can affect unchanged dependencies. Locally, use the same
scanner release:

```sh
osv-scanner scan source --lockfile=pom.xml --all-vulns
```

No vulnerability is ignored. If a finding is not exploitable here and cannot be fixed yet,
add it to an `osv-scanner.toml` `[[IgnoredVulns]]` entry with its ID, an `ignoreUntil` date
and a reason, in a reviewed PR. Maven build plugins and GitHub Actions are pinned and are not
scanned here; their updates are covered by #449.

## API compatibility

### Baseline

The baseline is the latest published release, `com.github.theholywaffle:teamspeak3-api:1.3.1`
from Maven Central. `verify` fetches it into `target/api-baseline/` and fails unless its
SHA-256 is `e76bfdb446c5d814a23ddd4f5f49486c1361fa61bcb2c8f393c7c13d96794756`. That JAR's
Central SHA-1 matches, and its signature is valid for the maintainer key
`72BF 8ED7 AA59 66B2 F5CA AA83 BEE1 8CF6 E79F 7B17`.

The artifact identity is unchanged in 2.0: groupId, artifactId, the
`com.github.theholywaffle.teamspeak3` packages and the `Automatic-Module-Name`
`com.github.theholywaffle.teamspeak3` all match 1.3.1. japicmp compares classes by name. If a
future release renames the artifact or a package, set `api.baseline.version` and the
coordinates explicitly. Expect every moved type to be reported as removed and added, and treat
that as a breaking change with migration notes, not an exclusion.

### What is compared

japicmp compares the public and protected API of every package, with no includes or
excludes. The `commands` packages are public until #447 hides them, so their changes count.
Reports are written to `target/japicmp/api-compatibility.{diff,md,html,xml}` and uploaded by the
**API compatibility** job.

- **Pending release (`verify`).** The library JAR is compared with 1.3.1. japicmp takes each
  version from the file name, so the new version is the pom version that release-please
  maintains (`2.0.0-SNAPSHOT`, then `2.0.0` in the release PR). The build fails if that version
  is too small for the changes found. For example, publishing this API as `1.4.0` is rejected
  (see the examples).
- **Each PR (CI).** The job builds the PR base, derives the release impact that release-please
  will apply from the PR title and body (`.github/scripts/api-impact.sh`), and compares base
  and head (`.github/scripts/api-compare.sh`). The job summary shows the impact, the result and
  the change list.

| Release impact | Title | Allowed API change |
| --- | --- | --- |
| major | `type!:` or a `BREAKING CHANGE:` footer | Any |
| minor | `feat:` | Binary- and source-compatible changes, such as additions |
| patch | `fix:`, `perf:`, `deps:`, `revert:` | None |
| none | `refactor:`, `test:`, `ci:`, `docs:`, `build:`, `chore:`, `style:` | None |

To check a change locally, build both JARs and run, for example:

```sh
bash .github/scripts/api-compare.sh base.jar target/teamspeak3-api-2.0.0-SNAPSHOT.jar \
  "$(bash .github/scripts/api-impact.sh 'feat: add lookup')" target/api-impact
```

### Controlled examples

`.github/scripts/test-api-compatibility.sh` compiles the fixture versions in
`src/test/api-fixtures/` and runs them through the project's own `api-compatibility`
execution. The examples check that:

- a removed method and a changed return type are reported, pass as major and are rejected as
  minor or patch;
- a method added to an interface is binary compatible but source incompatible, so it passes as
  major and is rejected as minor;
- an added method passes as minor and is rejected as patch or none;
- an implementation-only change passes as patch or none;
- a `fix:` title is mapped to patch and its API removal is rejected;
- the real library is rejected against 1.3.1 as `1.4.0` and accepted as the pending `2.0.0`.

It also tests the title/body mapping for each type. Run it after `./mvnw verify`.

### Intentional 2.0 breaks

The 1.3.1 comparison reports every 2.0 break, and the build accepts them only because the
pending version is a major release. Nothing is excluded. Every change since 1.3.1 stays
visible in the report artifact. Each break is reviewed when it lands, because the PR check
rejects an API break unless its title declares `!` or a `BREAKING CHANGE:` footer. The PR
then carries migration notes, which release-please copies into the 2.0.0 notes. The
operation-by-operation mapping is in the [domain API migration guide](api-migration.md),
and the other breaks are in the task guides linked from the README.

After 2.0.0 is published, set `api.baseline.version` to `2.0.0` and `api.baseline.sha256` to
the published JAR's checksum, and remove `release-as` from the release-please config. From then
on, the pending-release check enforces SemVer against 2.0.0 for every release.

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
| `TS3CommandFailedException` failed Java serialization (`QueryError` is not serializable) | It serializes; after deserialization `getErrorId()`, `getCommandName()`, `matchesQueryError()` and the message are kept, and `getError()` returns `null` |
| Communication logs and warnings contained passwords, privilege keys and file transfer keys | Those values are `<redacted>` |
| `Wrapper.toString()` and `DefaultArrayResponse.toString()` printed secrets | Secrets are `<redacted>`; use the getters or `getRawResponse()` for real values |

Release intent: `fix!:` for the planned **2.0.0**, with a `BREAKING CHANGE` footer listing the
rows above. Defaults, dependencies, transports and protocol are unchanged.
