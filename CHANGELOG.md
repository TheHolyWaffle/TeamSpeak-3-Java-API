# Changelog

Release-please maintains release notes from Conventional Commits. The first
modernization release is 2.0.0; its notes also cover tasks #429 and #430, which
merged before release automation existed. To upgrade from 1.x, follow the
[2.0 migration guide](MIGRATION.md).

## [2.0.0-beta.3](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/compare/v2.0.0-beta.2...v2.0.0-beta.3) (2026-10-31)


### Bug Fixes

* **deps:** update dependency com.google.code.gson:gson to v2.15.0 ([#524](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/524)) ([97aac38](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/97aac38b5608a3af56d29dfc3a0678f37a248930))
* **deps:** update dependency com.hierynomus:sshj to v0.41.2 ([#518](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/518)) ([3a3b30a](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/3a3b30aa9361f7b58c11dd0f73b70ffd0b5ed3da))

## [2.0.0-beta.2](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/compare/v2.0.0-beta.1...v2.0.0-beta.2) (2026-10-15)


### Features

* add temporary server passwords and client UID lookup ([#517](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/517)) ([8c32879](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/8c328794b616c0472fe2214942381f06f9d9fc93))
* complete server administration APIs and simplify listeners ([#515](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/515)) ([f4ca91c](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/f4ca91c58d3590c2a8d188ddc06107cf88c0f681))

## [2.0.0-beta.1](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/compare/v1.3.1...v2.0.0-beta.1) (2026-10-15)


### ⚠ BREAKING CHANGES

* channel, server group, channel group, virtual server, ban, offline message, icon and file transfer IDs are typed (`ChannelId`, `ServerGroupId`, `ChannelGroupId`, `VirtualServerId`, `BanId`, `MessageId`, `IconId`, …) in models, events and parameters, like `ClientId` and `ClientDatabaseId`; a `0` meaning "none" is an empty `Optional`. Icon IDs are unsigned `IconId` values, and `PermissionGrant.icon(..)` sets the `i_icon_id` permission. Ban durations are `Duration`s, with `Duration.ZERO` meaning permanent. `ServerQueryInfo.getId()` and `getVirtualServerPort()` are empty before a virtual server is selected. `permissions().assignments(..)` returns `PermissionSource` without value and flags; `permissions().overview(..)` still returns `PermissionAssignment`. Blank event invoker and reason text reads as empty, and `messages().getOffline` fails on a malformed response instead of reporting the message absent. See MIGRATION.md.
* releases are signed with a new OpenPGP key, `336C 77A1 6224 E1E6 90AE B70B 3B5F 5764 861E F0BA` (1.x used `72BF 8ED7 AA59 66B2 F5CA AA83 BEE1 8CF6 E79F 7B17`); update pinned keys before verifying signatures. A Gradle Module Metadata file is published next to the POM, and the JAR no longer contains `META-INF/maven` descriptors.
* the `with-dependencies` standalone JAR and the `full` Maven profile are removed, along with its bundled `simplelogger.properties`. Depend on `com.github.theholywaffle:teamspeak3-api` with Maven or Gradle and add one SLF4J 2.0 provider; without a build tool, collect the runtime dependencies, for example with `mvn dependency:copy-dependencies -DincludeScope=runtime`, and put them on the classpath.
* `TS3Query` is final; its constructor shares `this` with the event manager, so wrap a `TS3Query` instead of extending it. Passwords, privilege keys, temporary passwords, API keys and file transfer keys are replaced by `<redacted>` in DEBUG communication logs, warnings and `toString()`; getters and `getRawResponse()` keep the real values.
* the deprecated TS3Config.setCommandTimeout(int) is removed; use setCommandResponseTimeout with a Duration. The ReconnectingConnectionHandler constructor without maxAttempts is removed; pass the previous default, 10. Command parameter classes are records and Parameter is a sealed interface whose encoded() returns the wire text. Reader, writer and event threads are daemon threads, while the deadline scheduler is not, so an open query keeps the JVM alive until it is closed. See MIGRATION.md.
* response models and events are immutable and use typed values. Getters that returned -1, empty strings, false, null or Date now return Optional values, Instant, Duration, typed IDs, records or UNKNOWN, and missing required values fail with TS3InvalidValueException. Domain APIs take the typed IDs. FileTransferParameters.getQueryError() is replaced by findFailure(). See the models table in MIGRATION.md.
* `TS3Api` and `TS3ApiAsync` keep only session operations; everything else moved to domain APIs such as `channels()`, `clients()`, `permissions()` and `files()`, which take request objects instead of long nullable argument lists and return `Optional` or empty lists instead of `null` or `-1`. New exception types: `TS3TransportException` (now the parent of `TS3ConnectionFailedException` and `TS3QueryShutDownException`), `TS3TimeoutException` and `TS3UnsupportedOperationException`; `TS3Exception.isOutcomeUnknown()` tells whether a failed command may still have run. `TS3UnknownErrorException` and `TS3UnknownEventException` are removed. MIGRATION.md maps every 1.3.1 method.
* file sockets have finite deadlines, direct downloads and icon buffering default to 16 MiB, and cancellation aborts active file I/O. Icon stream preparation is asynchronous. Streams you pass are never closed by the library; behind NAT or Docker, configure a FileTransferEndpointResolver in FileTransferConfig. See MIGRATION.md.
* Remove ServerType and configuration generation accessors, simplify QueryTransportFactory.supports to Protocol, remove the TS6 timestamp-unit overload and launcher option. Configure TS3 with setProtocol and use the epoch-seconds file timestamp getter.
* BPermissionType.getId() and IPermissionType.getId() are removed; use permissions().idByName(enum.getName()) or permissions().list(). Unknown names in permissions().assignments(..) now propagate server errors.
* SSH no longer automatically trusts unknown keys. Provision known_hosts or select an explicit pin/TOFU policy with setSshHostKeyPolicy. TS3Config is final and remains reusable after query construction; queries capture independent QueryConfig snapshots. See MIGRATION.md.
* async APIs and Command.getFuture now return CompletableFuture. CommandFuture and its listener interfaces are removed; MIGRATION.md maps each CommandFuture method to its CompletableFuture replacement.
* command and listener admission is finite, excess commands fail with TS3QueueFullException, event overflow drops newest, removal discards waiting events, and identical event frames are delivered individually. See MIGRATION.md for the defaults and subscription handles.
* configure reconnect session state explicitly, opt in to safe-read replay, and migrate infinite reconnect assumptions to bounded attempts with jitter.
* connect, handshake, queue wait, command response and close each have a finite deadline; traffic no longer extends a pending command's deadline, and a response timeout closes the connection. `TS3Query` is `AutoCloseable` and cannot connect again once closed, `connect()` while connected throws, initialization callbacks run on owned virtual threads, and pending requests fail on termination instead of surviving a reconnect.
* Java 8–24 are no longer supported; classes are Java 25 bytecode ([#429](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/429)). The library uses the SLF4J 2.0 API with upgraded SSHJ and patched Bouncy Castle dependencies (#430); replace SLF4J 1.7 bindings with exactly one 2.0 provider.

### Features

* add `apiKeys()` to create, list and delete Web Query API keys over any transport ([#496](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/496)) ([5524215](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/552421545d25cf919ed16b9c371189c62787076e))
* add an optional TS3 Web Query transport ([#496](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/496)) ([5524215](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/552421545d25cf919ed16b9c371189c62787076e)), closes [#452](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/452)
* immutable response models with typed values ([#480](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/480)) ([26d1a99](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/26d1a9985a21458933cfda371d033a3d4e5aa201)), closes [#444](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/444)
* publish the regular JAR as the only library artifact ([#483](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/483)) ([053a39f](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/053a39ff59a8b47d05fe0180d1445761e7841444)), closes [#447](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/447)
* replace custom command futures with CompletableFuture ([#463](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/463)) ([ff67590](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/ff6759078af4f5fafedf0f94a74b785d1c4f82b5))
* share ServerQuery transports and snapshot configuration ([#464](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/464)) ([809fc2c](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/809fc2ce72d2606722b270fd5dac35632647a4be))
* split ServerQuery API into domain facades with coherent errors ([#479](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/479)) ([0c054e7](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/0c054e7c0def2787b159f7436dfe33d3afdaa44a)), closes [#443](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/443)
* typed IDs and consistent Optional handling ([#510](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/510)) ([5eff542](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/5eff542d45eb9fd8e03c7cc34efa6b0bab55220b))
* verify channel compatibility and expose controlled query commands ([#465](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/465)) ([0392c0f](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/0392c0f1b6b789eaf4477abae39d3972aabcdcbf)), closes [#441](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/441)
* verify permission compatibility using server metadata ([#466](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/466)) ([47fdc2d](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/47fdc2db013f33e8866d0f225c4771e278e4086d)), closes [#441](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/441)


### Bug Fixes

* `databaseClients().list()` returns an empty list on a virtual server without database clients instead of failing ([#496](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/496)) ([5524215](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/552421545d25cf919ed16b9c371189c62787076e))
* `exit()` waits for command capacity before sending `quit` instead of skipping it when every command slot is in use ([#496](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/496)) ([5524215](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/552421545d25cf919ed16b9c371189c62787076e))
* bound ServerQuery lifecycle and command deadlines ([#460](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/460)) ([a4d4585](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/a4d4585e32b26ac787b35377914274f633bcf285)), closes [#436](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/436)
* bound ServerQuery queues and isolate event callbacks ([#462](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/462)) ([0b971e6](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/0b971e601135ad6b72f2840cf7cafab476295ef0)), closes [#438](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/438)
* **deps:** add Gson 2.14.0 as a runtime dependency; modular applications using `TS3WebQuery` also need `requires java.net.http;` and `requires com.google.gson;` ([#496](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/496)) ([5524215](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/552421545d25cf919ed16b9c371189c62787076e))
* **deps:** update dependency org.bouncycastle:bc-jdk18on-bom to v1.86.2 ([#506](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/506)) ([9e1c542](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/9e1c5422f6a0e5f1727a45b2f14ba6be93810ca0))
* make ServerQuery reconnect and retries safe ([#461](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/461)) ([9b4e3f9](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/9b4e3f98f75f54d54f05a0d43b8c16b0e24620a2)), closes [#437](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/437)
* make TS3 file transfers bounded and resolve NAT endpoints ([#478](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/478)) ([c685be8](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/c685be8733abfa0ed1ecf6a527fbbde7c98ac445))
* preserve ServerQuery escapes and frame responses by protocol ([7ce30e5](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/7ce30e5c08388bb1bcd937262c08486d98142130))
* redact secrets from communication logs, diagnostics and toString() ([#482](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/482)) ([e421ee1](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/e421ee10566e7934d2d527f910830f60655aea16)), closes [#446](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/446)
* return target ID for server-group overwrite and verify copies ([#473](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/473)) ([c5675ba](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/c5675bae1ee795987a47a49fb0304e57c4e43c9d))
* verify TS3 command coverage and remove TS6 support ([#477](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/477)) ([e4be640](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/e4be6401536333afe4e43620e9b0da715f0b84f1))


### Code Refactoring

* Java 25 syntax and implementation cleanup ([#481](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/481)) ([de7a02d](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/de7a02d49f1c8898b078b8ad0043243e4843baa0)), closes [#445](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/445)


### Build System

* migrate the build from Maven to Gradle ([#502](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/502)) ([162f651](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/162f6513d9f26ad8e1eb9a64f0e2de1da8fa96ec)), closes [#486](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/486)
* require Java 25 and an SLF4J 2.0 provider ([a514981](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/commit/a514981e3382304f7720f768eb83904977f07607)), closes [#431](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues/431)

## [1.3.1](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/releases/tag/v1.3.1)

Last published release before modernization, verified against Maven Central metadata
and the GitHub release. Historical release notes remain on GitHub; the release
manifest uses 1.3.1 as its published baseline.
