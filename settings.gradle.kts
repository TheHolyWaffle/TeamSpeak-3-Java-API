rootProject.name = "teamspeak3-api"

// The formatter relies on JDK 25 javac internals.
check(JavaVersion.current().majorVersion == "25") {
    "Build with JDK 25 (found ${JavaVersion.current()}). Set JAVA_HOME to a JDK 25 installation."
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // The published release the API is compared with, under another group: with this project's own
        // coordinates, Gradle would resolve the project itself.
        exclusiveContent {
            forRepository {
                ivy("https://repo.maven.apache.org/maven2/") {
                    name = "apiBaseline"
                    patternLayout { artifact("com/github/theholywaffle/[module]/[revision]/[module]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("api-baseline") }
        }
    }
}
