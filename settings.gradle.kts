plugins {
    // Provisions the JDK 25 that gradle/gradle-daemon-jvm.properties requires, if none is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "teamspeak3-api"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
