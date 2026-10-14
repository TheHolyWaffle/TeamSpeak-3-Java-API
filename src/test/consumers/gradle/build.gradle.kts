// A classpath Gradle application using the staged library. Run by .github/scripts/consumer-check.sh.
plugins {
    application
}

val teamspeak3Version: String by project
val stagingRepository: String by project

repositories {
    // The library may only come from the staged repository, never a cache or Central.
    exclusiveContent {
        forRepository { maven { url = uri(stagingRepository) } }
        filter { includeGroup("com.github.theholywaffle") }
    }
    mavenCentral()
}

dependencies {
    implementation("com.github.theholywaffle:teamspeak3-api:$teamspeak3Version")
    // The application's logging choice; the library ships none.
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.20")
}

sourceSets {
    main {
        java {
            srcDir("../maven/src/main/java")
            exclude("module-info.java")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

application {
    mainClass = "consumer.ConsumerApp"
}

tasks.named<JavaExec>("run") {
    args("jdk14")
}
