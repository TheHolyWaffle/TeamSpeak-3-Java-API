# Packaging

Version 2.0 publishes one library artifact to Maven Central:

| File | Contents |
| --- | --- |
| `teamspeak3-api-<version>.jar` | The library classes and a manifest with `Automatic-Module-Name: com.github.theholywaffle.teamspeak3` and `Implementation-Version` |
| `teamspeak3-api-<version>-sources.jar` | Exactly the sources under `src/main/java` |
| `teamspeak3-api-<version>-javadoc.jar` | Javadoc for every library package |
| `teamspeak3-api-<version>.pom` | The POM generated from `build.gradle.kts`, with the project metadata Central requires |
| `teamspeak3-api-<version>.module` | [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html), the same dependencies for Gradle consumers |

The POM declares SSHJ, the SLF4J API and Gson (with its annotations-only
`error_prone_annotations` dependency) as compile dependencies and the patched Bouncy
Castle provider and PKIX modules as runtime dependencies. Their versions come from the
Bouncy Castle BOM, which Maven and Gradle both apply. Test-scoped dependencies are not
seen by consumers. No logging provider is a non-test dependency, so logging remains the
application's choice: add exactly one SLF4J 2.0 provider, or none to discard log output.

## Migration from the standalone JAR

Version 1.x also published `teamspeak3-api-<version>-with-dependencies.jar`, built by the
`full` Maven profile. Version 2.0 removes both the profile and that classifier. The
shaded JAR:

- bundled SimpleLogger and its provider registration, which forced a logging backend on
  applications and made a second provider produce SLF4J's multiple-provider warning;
- repackaged the Bouncy Castle provider without its JAR signature, which JDKs that
  require signed JCE providers reject;
- duplicated SSHJ, Bouncy Castle and SLF4J classes on any classpath that already had them,
  and needed separate review of merged service files and licenses.

Depend on `com.github.theholywaffle:teamspeak3-api` with Maven or Gradle instead, as the
[README](../README.md#installation) shows, and add your logging provider. Applications without a
build tool can collect the library and its runtime dependencies once:

```sh
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=lib \
  -Dmdep.useBaseVersion=true -f consumer-pom.xml
```

where `consumer-pom.xml` declares the library and the chosen provider, then run with
`java -cp "app.jar:lib/*" ...`. Gradle's `application` plugin (`installDist`) produces
the same layout. The removed `src/main/resources/simplelogger.properties` was only used
by the standalone JAR; [`src/example/simplelogger.properties`](../src/example/simplelogger.properties)
is the same configuration for applications using SimpleLogger.

## Java modules

The library keeps its `Automatic-Module-Name` instead of an explicit `module-info.class`.
Its dependencies were checked: Bouncy Castle 1.86 and SLF4J 2.0.20 ship module
descriptors, but SSHJ 0.41.1 (`com.hierynomus.sshj`) and its ASN.1 dependency
(`com.hierynomus.asnone`) are only automatic modules. An explicit descriptor would have to
require an automatic module, which `jlink` cannot link, and would also decide which
packages to encapsulate, which is an API change of its own. Revisit this once SSHJ
publishes a module descriptor.

A modular application declares `requires com.github.theholywaffle.teamspeak3;`.
The Bouncy Castle provider modules need no `--add-modules`: they declare
`provides java.security.Provider`, so service binding resolves them from the module path.
The Maven consumer check below runs on the module path and verifies this.

## Reproducible artifacts

Gradle writes archives without file timestamps, in a fixed entry order and with normalized
permissions, and Javadoc is generated with `-notimestamp`. The **gradle jreleaserDeploy -PdryRun** CI job
rebuilds the staged artifacts from a clean `build` directory, without the build cache, and requires
every file to be byte-identical. These are the unsigned artifacts; JReleaser adds signatures, which
are not reproducible, only at publication. Consumers can combine such a rebuild with the release's
provenance attestation; see [verify a published release](releases.md#verify-a-published-release).

## Consumer checks

[`consumer-check.sh`](../.github/scripts/consumer-check.sh) builds two applications in
`src/test/consumers` against the staged repository and runs them:

- **Maven**, as a named module on the module path, using SimpleLogger. Maven resolves
  from a dedicated local repository under `build/consumers`, so no cached or installed
  library can stand in for the staged one.
- **Gradle**, on the classpath, using `slf4j-jdk14`. An exclusive repository rule only
  allows the library from the staging repository.

Both run the same program. It connects over RAW to a loopback ServerQuery peer and reads
a typed `version` response, and sends a Web Query request to a loopback HTTP peer, checking
the API key header and the typed JSON result. The module-path consumer declares
`requires java.net.http` and `requires com.google.gson`, which modular applications using
`TS3WebQuery` need. It then attempts SSH against a non-SSH peer, which must fail
with `TS3ConnectionFailedException` after SSHJ has registered the runtime-scoped Bouncy
Castle provider. It checks that exactly the application's chosen SLF4J provider is
present and that the library's communication log reached it. The script then requires
that each consumer loaded a library JAR identical to the staged one.

Run the checks locally with JDK 25 and Maven on `PATH`, after staging a version:

```sh
./gradlew publish -Pversion=0.0.0
bash .github/scripts/consumer-check.sh 0.0.0
```

The **gradle jreleaserDeploy -PdryRun** CI job runs the same steps after signing the staged artifacts with a
disposable key and validating the Central bundle (`./gradlew jreleaserDeploy -PdryRun`).
