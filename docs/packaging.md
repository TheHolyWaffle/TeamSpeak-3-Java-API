# Packaging

Version 2.0 publishes one library artifact to Maven Central:

| File | Contents |
| --- | --- |
| `teamspeak3-api-<version>.jar` | The library classes, `META-INF/maven` descriptors and `Automatic-Module-Name: com.github.theholywaffle.teamspeak3` |
| `teamspeak3-api-<version>-sources.jar` | Exactly the sources under `src/main/java` |
| `teamspeak3-api-<version>-javadoc.jar` | Javadoc for every library package |
| `teamspeak3-api-<version>.pom` | The reviewed `pom.xml`, unchanged |

The POM declares SSHJ and the SLF4J API as compile dependencies and the patched Bouncy
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

`project.build.outputTimestamp` (`1980-01-01T00:00:02Z`) fixes every archive entry time,
and Javadoc is generated with `notimestamp`. During `validate`, the Maven Artifact Plugin's
`check-buildplan` goal fails the build if a plugin with a known reproducibility issue is in
the build plan. [`reproducibility-check.sh`](../.github/scripts/reproducibility-check.sh)
follows the [Maven reproducible builds guide](https://maven.apache.org/guides/mini/guide-reproducible-builds.html):
it installs a clean build into a temporary local repository, then runs
`clean verify artifact:compare` and requires the POM, library, sources and Javadoc JARs to
be byte-identical. As a negative control, a rebuild with another `outputTimestamp` must be
reported as different. The `.buildinfo` and `.buildcompare` fingerprints are kept under
`target/reproducibility`. These are the unsigned artifacts; JReleaser adds signatures,
which are not reproducible, only at publication. Consumers can combine such a rebuild with the release's
provenance attestation; see [verify a published release](releases.md#verify-a-published-release).

## Staged publication checks

[`verify-staged-artifacts.sh`](../.github/scripts/verify-staged-artifacts.sh) inspects
the local `-Pcentral-staging deploy` repository that JReleaser signs and bundles:

- the staging directory for the given version holds exactly the POM and the library, sources and Javadoc JARs,
  so no shaded or other classifier is published, and the staged POM equals `pom.xml`;
- the library JAR holds only project classes and its Maven descriptors, with no third-party
  classes, service registrations or signature files, keeps the automatic module name,
  embeds the given version and contains only Java 25 bytecode without preview features;
- the sources JAR matches `src/main/java`, and the Javadoc JAR documents every package
  without generation dates;
- every entry of every JAR carries the configured `outputTimestamp`;
- when given JReleaser's Central bundle, it holds exactly those four files with their
  signatures and MD5/SHA-1/SHA-256/SHA-512 checksums, byte-identical to the staged files.

The given version is the one `release-metadata.sh` has already approved: it requires the
POM version, `.release-please-manifest.json`, the newest changelog entry and the tag to agree.

[`test-verify-staged-artifacts.sh`](../.github/scripts/test-verify-staged-artifacts.sh)
runs the verifier, then copies the valid staged repository and bundle and introduces one
defect per copy. It requires each copy to be rejected for the intended reason: a shaded classifier,
a missing Javadoc JAR, a changed POM, an unnormalized timestamp, a bundled logging
configuration, preview bytecode, incomplete sources, and a bundle with a missing
signature or a different library JAR.

JReleaser's Central rules separately validate the POM metadata (name, description, URL,
license, developers and SCM) and require sources and Javadoc. The release rehearsal checks
every signature and that a bundle missing its sources is rejected. See
[CI and releases](releases.md#local-validation-and-ci-rehearsal).

## Consumer checks

[`consumer-check.sh`](../.github/scripts/consumer-check.sh) builds two applications in
`src/test/consumers` against the staged repository and runs them:

- **Maven**, as a named module on the module path, using SimpleLogger. Maven resolves
  from a dedicated local repository under `target/consumers`, so no cached or installed
  library can stand in for the staged one.
- **Gradle**, on the classpath, using `slf4j-jdk14`. An exclusive repository rule only
  allows the library from the staging repository.

Both run the same program. It connects over RAW to a loopback ServerQuery peer and reads
a typed `version` response, and sends a Web Query request to a loopback HTTP peer, checking
the API key header and the typed JSON result. The module-path consumer declares
`requires java.net.http`, which modular applications using `TS3WebQuery` need. It then attempts SSH against a non-SSH peer, which must fail
with `TS3ConnectionFailedException` after SSHJ has registered the runtime-scoped Bouncy
Castle provider. It checks that exactly the application's chosen SLF4J provider is
present and that the library's communication log reached it. The script then requires
that each consumer loaded a library JAR identical to the staged one.

Run the checks locally in a disposable copy of the repository with JDK 25, GnuPG and the
Gradle version in `src/test/consumers/gradle/gradle/wrapper/gradle-wrapper.properties` (on
`PATH`, or set `GRADLE`):

```sh
bash .github/scripts/synthetic-release.sh 0.0.0
bash .github/scripts/release-rehearsal.sh
bash .github/scripts/test-verify-staged-artifacts.sh 0.0.0 target/staging-deploy \
  target/jreleaser/deploy/mavenCentral/central/com.github.theholywaffle-teamspeak3-api-0.0.0-bundle.zip
bash .github/scripts/consumer-check.sh 0.0.0
bash .github/scripts/reproducibility-check.sh
```

[`synthetic-release.sh`](../.github/scripts/synthetic-release.sh) applies what a
release-please PR changes: POM version, manifest and a newest changelog entry. It then
extracts the release notes through the same gate as a real release. The JReleaser
release dry-run in the rehearsal needs a Git checkout.

The **Validate publication bundle (no upload)** CI job runs all but the last step;
**Reproducible unsigned artifacts** runs the last. The explicit release workflow runs
`verify-staged-artifacts.sh` with the approved version before JReleaser signs or deploys.
