import java.security.MessageDigest
import java.time.Duration
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.errorprone)
    alias(libs.plugins.spotless)
}

group = "com.github.theholywaffle"
description = "A Java wrapper of the TeamSpeak 3 Server Query API"

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    test {
        // Examples compile with the tests, so every build catches API drift; they are never packaged.
        java.srcDir("src/example/java")
    }
}

dependencies {
    api(libs.sshj)
    api(libs.slf4j.api)
    api(libs.gson)
    // SSHJ 0.41.1 uses BC 1.84, affected by CVE-2026-8763 and CVE-2026-13506. The BOM aligns provider,
    // PKIX and utility modules; the direct declarations reach consumers, which the BOM alone does not.
    implementation(platform(libs.bouncycastle.bom))
    runtimeOnly(libs.bouncycastle.prov)
    runtimeOnly(libs.bouncycastle.pkix)

    errorprone(libs.errorprone.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    // The TS3 container fixture issues its own TLS certificates.
    testImplementation(libs.bouncycastle.pkix)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-Xmaxwarns", "10000"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        // Summaries are optional: many members are fully described by @return or @see.
        disable("MissingSummary")
    }
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("-Xdoclint:all/protected", "-Xdoclint:-missing"))
}

tasks.compileTestJava {
    // Test fixtures close servers whose close() may be interrupted, and tests start futures only to
    // observe their side effects.
    options.compilerArgs.add("-Xlint:-try")
    options.errorprone.disable("FutureReturnValueIgnored")
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "com.github.theholywaffle.teamspeak3",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        memberLevel = JavadocMemberLevel.PROTECTED
        windowTitle = "TeamSpeak 3 Java API $version API"
        docTitle = windowTitle
        bottom = "Released under the MIT license."
        isLinkSource = true
        noTimestamp(true)
        tags("querycommands:m:Number of query commands sent:")
        // Same policy as the compiler's -Xdoclint.
        addBooleanOption("Xdoclint:all,-missing", true)
        addBooleanOption("Xwerror", true)
        addStringOption("-release", "25")
        quiet()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}

tasks.test {
    useJUnitPlatform { excludeTags("debug-logging") }
    exclude("**/*IT.class", "**/*IT$*.class")
}

// A separate JVM, so slf4j-simple starts with DEBUG output to a file.
val debugLoggingTest = tasks.register<Test>("debugLoggingTest") {
    description = "Runs the tests that inspect DEBUG communication logging."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("debug-logging") }
    exclude("**/*IT.class", "**/*IT$*.class")
    val logFile = layout.buildDirectory.file("debug-communication.log")
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    systemProperty("org.slf4j.simpleLogger.logFile", logFile.get().asFile.path)
}

// The authenticated ServerQuery suite against the pinned TS3 image; needs Docker.
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the integration tests against a TeamSpeak 3 server container."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/*IT.class")
    timeout = Duration.ofMinutes(10)
    // The result depends on a live server, and the evidence in build/compatibility is not a task output.
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("it verifies a live server") { true }
    shouldRunAfter(tasks.test, debugLoggingTest)
    finalizedBy("jacocoIntegrationTestReport")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    executionData(tasks.test.get(), debugLoggingTest.get())
}

tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    executionData(integrationTest.get())
    sourceSets(sourceSets.main.get())
}

tasks.jacocoTestCoverageVerification {
    executionData(tasks.test.get(), debugLoggingTest.get())
    // Per-class floors for protocol, lifecycle and error-path code; facades and models are covered by
    // the TS3 integration suite instead of a global percentage.
    val racy = listOf(
        "com.github.theholywaffle.teamspeak3.api.CommandFutures.RequestFuture",
        "com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectingConnectionHandler",
        "com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy.UserControlled",
        "com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration.Subscription",
    )
    fun JacocoViolationRule.floors(branches: String) {
        element = "CLASS"
        limit {
            counter = "LINE"
            minimum = "0.80".toBigDecimal()
        }
        limit {
            counter = "BRANCH"
            minimum = branches.toBigDecimal()
        }
    }
    violationRules {
        rule {
            floors("0.75")
            includes = listOf(
                "CommandQueue*",
                "Connection",
                "Deadline",
                "KeepAlive",
                "ProtocolLineReader",
                "StreamReader",
                "StreamWriter",
                "TS3Query",
                "EventManager*",
                "FileTransferHelper*",
                "QueryConfig",
                "SshHostKeyPolicy*",
                "TS3WebQuery",
                "WebQuery*",
                "ServerQueryBackend",
                "api.exception.TS3AuthenticationFailedException",
                "api.exception.TS3HttpException",
                "api.CommandFutures*",
                "api.exception.TS3CommandFailedException",
                "api.exception.TS3TimeoutException",
                "api.exception.TS3InvalidValueException",
                "api.reconnect.*",
                "api.wrapper.QueryError",
                "commands.Command",
                "commands.CommandBuilder",
                "commands.CommandEncoding",
                "commands.ProtocolRedactor",
                "commands.parameter.*",
                "commands.response.*",
            ).map { "com.github.theholywaffle.teamspeak3.$it" }
            excludes = racy
        }
        rule {
            // Remaining branches are concurrent state races, exercised by the integration suite.
            floors("0.50")
            includes = racy
        }
    }
}

// API compatibility with the latest published release, verified by checksum. The pending version must
// cover the change; japicmp reads each version from the file name.
val apiBaselineVersion = "1.3.1"
val apiBaselineSha256 = "e76bfdb446c5d814a23ddd4f5f49486c1361fa61bcb2c8f393c7c13d96794756"

val japicmpTool = configurations.create("japicmpTool")
val apiBaseline = configurations.create("apiBaseline") { isTransitive = false }

dependencies {
    japicmpTool(variantOf(libs.japicmp) { classifier("jar-with-dependencies") }) { isTransitive = false }
    apiBaseline("api-baseline:teamspeak3-api:$apiBaselineVersion")
}

val japicmp = tasks.register<JavaExec>("japicmp") {
    description = "Checks the API against the $apiBaselineVersion release under semantic versioning."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    val baseline = files(apiBaseline)
    val expectedSha256 = apiBaselineSha256
    val oldJar = layout.buildDirectory.file("api-baseline/teamspeak3-api-$apiBaselineVersion.jar")
    val newJar = tasks.jar.flatMap { it.archiveFile }
    val dependencies = files(configurations.runtimeClasspath)
    val report = layout.buildDirectory.file("japicmp/api-compatibility.diff")
    inputs.files(baseline, newJar, dependencies)
    outputs.file(report)
    classpath = files(japicmpTool)
    mainClass = "japicmp.JApiCmp"
    argumentProviders.add {
        listOf(
            "--old", oldJar.get().asFile.path, "--new", newJar.get().asFile.path,
            "--old-classpath", dependencies.asPath, "--new-classpath", dependencies.asPath,
            "-a", "protected", "--only-modified", "--report-only-filename", "--error-on-semantic-incompatibility",
        )
    }
    doFirst {
        val jar = baseline.singleFile
        val sha256 = MessageDigest.getInstance("SHA-256").digest(jar.readBytes()).joinToString("") { "%02x".format(it) }
        check(sha256 == expectedSha256) { "The API baseline $jar does not match the verified published artifact." }
        jar.copyTo(oldJar.get().asFile, overwrite = true)
        standardOutput = report.get().asFile.apply { parentFile.mkdirs() }.outputStream()
    }
}

tasks.check {
    dependsOn(debugLoggingTest, tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification, japicmp)
}

spotless {
    java {
        target(
            "src/main/java/**/*.java",
            "src/test/java/**/*.java",
            "src/example/java/**/*.java",
            "src/test/api-fixtures/**/*.java",
            "src/test/consumers/**/*.java",
        )
        palantirJavaFormat(libs.versions.palantir.java.format.get())
    }
    format("misc") {
        target(
            ".github/**/*.yml",
            ".github/**/*.sh",
            ".github/**/*.json5",
            "src/**/*.properties",
            "src/**/*.sh",
            "src/test/consumers/**/*.xml",
            "src/test/consumers/**/*.kts",
            "*.yml",
            "*.json",
            "*.kts",
            "gradle.properties",
            "gradle/*.toml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Development-only JVMs on the test classpath.
tasks.register<JavaExec>("devServer") {
    description = "Starts a disposable TS3 server; -PdevOnce=true exits after its smoke test."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.github.theholywaffle.teamspeak3.DevelopmentServer"
    args(providers.gradleProperty("devServer").orElse("ts3").get(), providers.gradleProperty("devOnce").orElse("false").get())
    standardInput = System.`in`
}

tasks.register<JavaExec>("runExample") {
    description = "Runs one example (-Pexample=ClientInfoExample) with the TS3_* environment variables."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.github.theholywaffle.teamspeak3.example." + providers.gradleProperty("example").orElse("ClientInfoExample").get()
    // SSHJ logs connection details at INFO and a harmless warning when the server closes the channel.
    jvmArgs("-Dorg.slf4j.simpleLogger.log.net.schmizz=error")
    standardInput = System.`in`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "TeamSpeak 3 Java API"
                description = project.description
                url = "https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "TheHolyWaffle"
                        name = "Bert De geyter"
                        email = "degeyter.bert@gmail.com"
                        url = "https://github.com/TheHolyWaffle"
                    }
                    developer {
                        id = "rogermb"
                        name = "Roger Baumgartner"
                        email = "rogermarcbaumgartner@hotmail.com"
                        url = "https://github.com/rogermb"
                        timezone = "+1"
                    }
                }
                scm {
                    url = "https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API"
                    connection = "scm:git:https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API.git"
                    developerConnection = "scm:git:git@github.com:TheHolyWaffle/TeamSpeak-3-Java-API.git"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/issues"
                }
            }
        }
    }
    repositories {
        // JReleaser signs, bundles and uploads this directory to Central.
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

val jreleaserTool = configurations.create("jreleaserTool")

dependencies {
    jreleaserTool(libs.jreleaser)
}

// Signs the staged publication and deploys it to Central; -PdryRun validates the bundle without uploading.
tasks.register<JavaExec>("jreleaserDeploy") {
    description = "Signs and deploys build/staging-deploy to Maven Central with JReleaser."
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    dependsOn(tasks.named("publishMavenPublicationToStagingRepository"))
    classpath = files(jreleaserTool)
    mainClass = "org.jreleaser.tool.JReleaser"
    args("deploy", "--config-file", "jreleaser.yml", "--output-directory", "build")
    if (providers.gradleProperty("dryRun").isPresent) args("--dry-run")
    environment("JRELEASER_PROJECT_VERSION", version.toString())
}
