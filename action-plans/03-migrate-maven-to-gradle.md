# Action Plan: Migrate from Maven to Gradle

**Priority**: 3
**Estimated Effort**: Medium
**Dependencies**: Java 17 upgrade (Action Plan 02)
**Risk Level**: Medium

## Objective

Migrate the build system from Maven to Gradle to improve build performance, dependency management, and developer experience.

## Rationale

While Maven is functional, Gradle offers significant advantages for modern Java development:

1. **Performance**: Gradle builds are typically 2-10x faster due to incremental compilation and build caching
2. **Flexibility**: More flexible build scripts with Groovy/Kotlin DSL
3. **Dependency Management**: Better conflict resolution and dependency insights
4. **Modern Features**: Built-in support for multi-module projects, composite builds
5. **Ecosystem**: Better integration with modern development tools and IDEs
6. **Incremental Builds**: Only rebuilds what changed, significantly faster development cycles

## Current State Analysis

- **Build Tool**: Maven 3.x with pom.xml
- **Plugins**: Compiler, Surefire, JAR, GPG, Nexus Staging, Shade
- **Profiles**: Default, full (with dependencies), ossrh (publishing)
- **Dependencies**: sshj, slf4j, junit
- **Publishing**: Maven Central via OSSRH

## Benefits of Gradle

### Performance Improvements
- **Build Cache**: Reuse outputs from previous builds
- **Incremental Compilation**: Only compile changed files
- **Parallel Execution**: Execute tasks in parallel
- **Daemon Process**: Persistent JVM for faster startup

### Developer Experience
- **Flexible DSL**: More readable and maintainable build scripts
- **Better IDE Integration**: Superior support in IntelliJ IDEA and VS Code
- **Rich Plugin Ecosystem**: Extensive plugin library
- **Composite Builds**: Better multi-project support

### Dependency Management
- **Version Catalogs**: Centralized dependency management
- **Dependency Insights**: Better conflict resolution visualization
- **Platform Dependencies**: BOM-like dependency management
- **Lock Files**: Reproducible builds with dependency locking

## Implementation Plan

### Phase 1: Gradle Setup and Basic Migration
1. **Initialize Gradle Wrapper**
   - Add gradle wrapper files
   - Configure gradle.properties
   - Set up basic build.gradle.kts (Kotlin DSL)

2. **Migrate Basic Configuration**
   - Project metadata (group, version, description)
   - Java compilation settings
   - Source sets configuration

3. **Dependency Migration**
   - Convert Maven dependencies to Gradle format
   - Set up version catalog for dependency management
   - Configure test dependencies

### Phase 2: Plugin and Task Migration
1. **Core Plugins**
   - Java plugin configuration
   - Maven publishing plugin
   - Signing plugin for GPG
   - Shadow plugin (equivalent to Maven Shade)

2. **Testing Configuration**
   - JUnit 5 test configuration
   - Test reporting and coverage
   - Integration test setup

3. **Quality Plugins**
   - SpotBugs for static analysis
   - Checkstyle for code style
   - JaCoCo for code coverage

### Phase 3: Publishing and Distribution
1. **Maven Central Publishing**
   - Configure maven-publish plugin
   - Set up signing for artifacts
   - Configure OSSRH repository

2. **Artifact Generation**
   - JAR with dependencies (shadow plugin)
   - Sources JAR
   - Javadoc JAR
   - POM generation

### Phase 4: Advanced Features and Optimization
1. **Build Performance**
   - Enable build cache
   - Configure parallel execution
   - Optimize task dependencies

2. **Development Workflow**
   - Continuous build mode
   - Test filtering and selection
   - Custom tasks for common operations

## Technical Specifications

### Project Structure
```
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml
├── src/
│   ├── main/java/
│   ├── test/java/
│   └── integrationTest/java/
└── example/
```

### build.gradle.kts (Main Configuration)
```kotlin
plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.github.theholywaffle"
version = "1.4.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(libs.sshj)
    api(libs.slf4j.api)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj.core)
    testImplementation(libs.testcontainers.junit)
    testRuntimeOnly(libs.slf4j.simple)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
        
        val integrationTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(libs.testcontainers.junit)
            }
        }
    }
}
```

### gradle/libs.versions.toml (Version Catalog)
```toml
[versions]
sshj = "0.35.0"
slf4j = "1.7.35"
junit = "5.10.1"
mockito = "5.7.0"
assertj = "3.24.2"
testcontainers = "1.19.3"

[libraries]
sshj = { module = "com.hierynomus:sshj", version.ref = "sshj" }
slf4j-api = { module = "org.slf4j:slf4j-api", version.ref = "slf4j" }
slf4j-simple = { module = "org.slf4j:slf4j-simple", version.ref = "slf4j" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
testcontainers-junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }

[plugins]
shadow = { id = "com.github.johnrengelman.shadow", version = "8.1.1" }
```

### Publishing Configuration
```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("TeamSpeak 3 Java API")
                description.set("A Java wrapper of the TeamSpeak 3 Server Query API")
                url.set("https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API")
                
                licenses {
                    license {
                        name.set("MIT")
                        url.set("http://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("TheHolyWaffle")
                        name.set("Bert De geyter")
                        email.set("degeyter.bert@gmail.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git@github.com:TheHolyWaffle/TeamSpeak-3-Java-API.git")
                    developerConnection.set("scm:git:git@github.com:TheHolyWaffle/TeamSpeak-3-Java-API.git")
                    url.set("https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
```

## Migration Strategy

### Parallel Development
1. **Maintain Maven Build**: Keep pom.xml functional during migration
2. **Gradual Migration**: Migrate features incrementally
3. **Validation**: Ensure both builds produce identical artifacts
4. **Documentation**: Update build instructions progressively

### Testing Strategy
1. **Build Verification**: Compare Maven and Gradle outputs
2. **Performance Testing**: Measure build time improvements
3. **CI/CD Testing**: Verify GitHub Actions work with Gradle
4. **Integration Testing**: Ensure published artifacts work correctly

## Risk Mitigation

1. **Backward Compatibility**
   - Keep Maven build until Gradle is fully validated
   - Ensure artifact compatibility
   - Maintain same publishing coordinates

2. **Team Adoption**
   - Provide Gradle training materials
   - Document common tasks and equivalents
   - Create migration guide for contributors

3. **CI/CD Integration**
   - Update GitHub Actions gradually
   - Test publishing pipeline thoroughly
   - Maintain rollback capability

## Acceptance Criteria

- [ ] Gradle build produces identical artifacts to Maven
- [ ] All Maven plugins have Gradle equivalents configured
- [ ] Publishing to Maven Central works correctly
- [ ] CI/CD pipeline updated and functional
- [ ] Build performance improved (target: 30-50% faster)
- [ ] Documentation updated with Gradle instructions
- [ ] Developer workflow improved

## Performance Expectations

- **Clean Build**: 30-50% faster than Maven
- **Incremental Build**: 70-90% faster than Maven
- **Test Execution**: 20-40% faster with parallel execution
- **IDE Integration**: Faster import and sync times

## Breaking Changes

- **Build Tool**: Developers need Gradle instead of Maven
- **Build Commands**: Different command syntax
- **IDE Configuration**: May require IDE reconfiguration

## Success Metrics

- Build time reduction: Target 30-50% improvement
- Developer satisfaction: Improved build experience
- CI/CD efficiency: Faster pipeline execution
- Maintenance: Easier build script maintenance

## Next Steps

After completion, this migration enables:
- Faster development cycles
- Better dependency management
- Modern build features and plugins
- Improved developer experience
- Foundation for advanced build optimizations
