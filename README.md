TeamSpeak 3 Java API
====================
[![Build Status](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/actions/workflows/maven.yml/badge.svg?branch=modernization%2F2.0)](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/actions/workflows/maven.yml) [![Maven Central](https://img.shields.io/maven-central/v/com.github.theholywaffle/teamspeak3-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.theholywaffle%22%20a%3A%22teamspeak3-api%22) [![Javadocs](http://www.javadoc.io/badge/com.github.theholywaffle/teamspeak3-api.svg)](http://www.javadoc.io/doc/com.github.theholywaffle/teamspeak3-api) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

A Java wrapper of the [TeamSpeak 3](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf)  Server Query API

For the 2.0 asynchronous API, see the [CompletableFuture migration and execution contract](docs/futures.md).

## Features

- Contains almost all server query functionality! (see [TeamSpeak 3 Server Query Manual](https://www.teamspeak-info.de/downloads/ts3_serverquery_manual.pdf))
- Built-in keep alive method
- Threaded event-based system
- Both [synchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) and [asynchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3ApiAsync.java) implementations available
- Can be set up to reconnect and automatically resume execution after a connection problem
- Utilizes [SLF4J](https://www.slf4j.org/) for logging abstraction and integrates with your logging configuration

## Getting Started

### Download

- **Option 1 (Standalone Jar)**: 

    Download the [latest release](https://search.maven.org/remote_content?g=com.github.theholywaffle&a=teamspeak3-api&v=LATEST&c=with-dependencies) and add this jar to the buildpath of your project.

- **Option 2 (Maven)**: 

    Add the following to your pom.xml:

    ```xml
    <dependency>
	    <groupId>com.github.theholywaffle</groupId>
	    <artifactId>teamspeak3-api</artifactId>
	    <version>...</version>
    </dependency>
    ```

    This API utilizes [SLF4J](https://www.slf4j.org/) for logging purposes and doesn't come shipped with a default logging implementation, if you use Maven instead of the standalone jar.
    You will manually have to add one via Maven to get any logging going, if you don't have one already. 
    
    The easiest way to do so is to just add SimpleLogger to your project, which also supports configuration via
    config file (needs to be shipped as a resource with your jar), if you want to log ```DEBUG``` messages for instance (e.g. raw client-server communication). 

    See this [configuration example](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/blob/master/src/main/resources/simplelogger.properties) for SimpleLogger.
    Add the following to your pom.xml to get started:

    ```xml
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>2.0.20</version>
    </dependency>
    ```

    This development branch uses SLF4J 2.0.20. Choose exactly one provider that supports
    the SLF4J 2.0 API, and keep `slf4j-api` and `slf4j-simple` at the same version when
    using SimpleLogger. SLF4J 2.x discovers providers through `ServiceLoader`; old
    SLF4J 1.7 bindings are ignored and must be replaced. Existing SLF4J logging calls
    remain compatible. See the [SLF4J manual](https://slf4j.org/manual.html).

    The Maven library supplies only the logging API. The standalone
    `with-dependencies` JAR includes SimpleLogger 2.0.20 and its provider registration;
    use the Maven library when supplying your own logging provider.


### Usage

All functionality is contained in the [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.

1. Create a [TS3Config](src/main/java/com/github/theholywaffle/teamspeak3/TS3Config.java) object and customize it.
2. Create a [TS3Query](src/main/java/com/github/theholywaffle/teamspeak3/TS3Query.java) object with your TS3Config as argument.
3. Call `TS3Query#connect()` to connect to the server.
4. Call `TS3Query#getApi()` to get an [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.
5. Do whatever you want with this api :)
6. Close the query, preferably with try-with-resources. See the [2.0 lifecycle and deadline migration notes](docs/connection-lifecycle.md) and [safe reconnect and retry](docs/reconnect-and-retry.md), and [event/capacity migration notes](docs/events-and-capacity.md).


### Example

```java
final TS3Config config = new TS3Config();
config.setHost("77.77.77.77");

final TS3Query query = new TS3Query(config);
query.connect();

final TS3Api api = query.getApi();
api.login("serveradmin", "serveradminpassword");
api.selectVirtualServerById(1);
api.setNickname("PutPutBot");
api.sendChannelMessage("PutPutBot is online!");
...
```

### More examples

[here](example)

## Building from source

This development branch requires **JDK 25 or newer** to build and run the library. It produces
Java 25 bytecode; Java 8–24 runtimes used with earlier versions are no longer supported.

Install a JDK 25 distribution (for example, [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=25))
and set `JAVA_HOME` to its installation directory. On macOS, after installing JDK 25:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
```

On Linux, set `JAVA_HOME` to your JDK 25 directory (for example,
`export JAVA_HOME=/path/to/jdk-25`). On Windows, set `JAVA_HOME` to the JDK 25 directory
and use `mvnw.cmd` in place of `./mvnw`.

From a fresh checkout, run:

```sh
./mvnw -v
./mvnw -B -ntp verify
```

The checked-in Maven Wrapper downloads and verifies the pinned Maven 3.10.0
archive on first use, so a separate Maven installation is unnecessary. The first
build requires internet access to download Maven, plugins, and dependencies.
Check that `./mvnw -v` reports Java 25 or newer. CI verifies Java 25 and Java 27.
Maven Enforcer rejects older build JDKs and unpinned Maven versions during
validation, before compilation. No `toolchains.xml` is needed: compilation, tests, and Javadoc use the JDK running Maven.

The default build runs the unit tests and creates the normal JAR, sources JAR and
Java 25 Javadoc JAR under `target`. To also build the standalone JAR:

```sh
./mvnw -B -ntp -Pfull verify
```

Docker-dependent TS3 raw, TS3 SSH and TS6 SSH smoke tests run separately:

```sh
./mvnw -B -ntp -Pintegration verify
```

See the [command/event compatibility inventory](docs/command-compatibility.md) for
verified domains, server differences and the controlled command escape hatch.

See [integration tests](docs/integration-tests.md) for pinned official images,
license configuration, Apple Silicon emulation requirements and beta revalidation.
The profile fails when required Docker tests cannot run; ordinary unit builds
need no Docker.

To start a disposable local server and run a compiled Java example:

```sh
./mvnw -ntp -Pdev-server test-compile exec:exec
```

Add `-Ddev.server=ts6` for TS6. The launcher prints connection details and keeps
the server running until Enter or Ctrl+C. See [local development server](docs/development-server.md)
for stop/reset behavior, noninteractive runs and fixture requirements.

These commands build locally and do not publish artifacts. Modernization tasks
branch from and merge into `modernization/2.0`; `master` remains the default branch
and receives the final validated integration PR. Ordinary merges never publish
with these workflows. Publication uses an explicit, protected JReleaser action from the approved stable
release commit on `master`; ordinary pushes and integration merges do not publish.

### Release preparation

Release-please prepares Maven release PRs from Conventional Commit messages.
Use a Conventional Commit PR title and squash-merge it: `fix: ...` for a patch,
`feat: ...` for a feature, and `feat!: ...` (or another type with `!`) for a
breaking change. Include migration details in the PR body and reference its issue.
Internal changes use titles such as `ci: ...`, `test: ...`, or `chore: ...`.
No separate release fragments or local release tooling are required.

Run **Prepare release** manually on `modernization/2.0` to open or update its
release PR. The first modernization release is pinned to **2.0.0**. See
[release preparation and CI administration](docs/releases.md) for the token,
merge settings, snapshot convention, and JReleaser Central Portal publication and recovery.

## Extra notes

### SSH security defaults

SSH queries require credentials supplied through `TS3Config#setLoginCredentials`.
SSHJ tries password authentication, then keyboard-interactive using the same password.
Unknown and changed host keys are rejected by default using `~/.ssh/known_hosts`.
Use `SshHostKeyPolicy.knownHosts(path)` for a dedicated strict trust file,
`pinnedKey(publicKey)` for an independently verified key, or explicitly choose
`trustOnFirstUse(path)` for a suitable environment. TOFU requires an application-owned
file and rejects changes, including a different key algorithm, after first trust.
The library never creates a trust directory or silently falls back to the working directory.

Configuration is captured in an immutable `QueryConfig` snapshot without freezing
`TS3Config`. Set `ServerType.TS6` and `Protocol.SSH` for TS6; TS3 retains raw TCP and
uses the same SSH implementation. See [configuration and transport migration](docs/configuration-and-transports.md).

The client uses SSHJ's default algorithms, which still include legacy algorithms;
this update does not enforce a modern-only algorithm policy. Compression is disabled.
Terrapin's strict key-exchange mitigation also requires server support.

### FloodRate

Only use `FloodRate.UNLIMITED` if you are sure that your query account is whitelisted (query_ip_whitelist.txt in Teamspeak server folder). If not, use `FloodRate.DEFAULT`. The server will temporarily ban your account if you send too many commands in a short period of time. For more info on this, check the [TeamSpeak 3 Server Query Manual, page 6](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf#page=6).

### TS3Config Settings

| Option | Description | Method signature | Default value | Required |
| --- | --- | --- | :---: | :---: |
|Host/IP | IP/Host of TeamSpeak 3 server.| ``setHost(String)`` |  | yes |
|QueryPort | Query port of TeamSpeak 3 server. | ``setQueryPort(int)`` | 10011 | no |
|FloodRate | Prevents possible spam to the server. | ``setFloodRate(FloodRate)`` | `FloodRate.DEFAULT` | no |
|Communications logging | Log client-server communication. | ``setEnableCommunicationsLogging(boolean)`` | false | no |
|Command timeout | Time until a command waiting for a response fails | ``setCommandTimeout(int)`` | 4000 (ms) | no |

## Questions or bugs?

Please let us know [here](../../issues). We'll try to help you as soon as we can.

If you just have a simple question or want to talk to us about something else, please join the [repository chat](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API) on Gitter.
