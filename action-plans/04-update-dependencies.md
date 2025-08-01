# Action Plan: Update Dependencies

**Priority**: 4
**Estimated Effort**: Low-Medium
**Dependencies**: Test infrastructure (Action Plan 01), Java 17 upgrade (Action Plan 02)
**Risk Level**: Low-Medium

## Objective

Update all project dependencies to their latest stable versions to improve security, performance, and access to new features while maintaining compatibility.

## Rationale

Outdated dependencies pose several risks and missed opportunities:

1. **Security Vulnerabilities**: Older versions may contain known security issues
2. **Performance**: Newer versions often include performance improvements
3. **Bug Fixes**: Missing important bug fixes and stability improvements
4. **Feature Access**: Unable to leverage new features and capabilities
5. **Ecosystem Compatibility**: Potential conflicts with other modern libraries
6. **Maintenance Burden**: Harder to get support for outdated versions

## Current State Analysis

### Current Dependencies
```xml
<!-- Main Dependencies -->
<dependency>
    <groupId>com.hierynomus</groupId>
    <artifactId>sshj</artifactId>
    <version>0.35.0</version> <!-- Released: 2023-01-15 -->
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.35</version> <!-- Released: 2022-01-21 -->
</dependency>

<!-- Test Dependencies -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version> <!-- Released: 2021-02-13 -->
</dependency>
```

### Dependency Analysis
- **sshj**: Moderately recent, but newer versions available
- **slf4j**: Outdated, version 2.x available with improvements
- **junit**: Very outdated, JUnit 5 is the current standard

## Implementation Plan

### Phase 1: Dependency Audit and Planning
1. **Security Scan**
   - Run dependency vulnerability scan
   - Identify critical security updates
   - Prioritize security-related updates

2. **Compatibility Analysis**
   - Check breaking changes in new versions
   - Identify API changes that affect the codebase
   - Plan migration strategies for breaking changes

3. **Version Selection**
   - Choose appropriate target versions
   - Consider LTS vs latest versions
   - Ensure Java 17 compatibility

### Phase 2: Core Dependencies Update
1. **SLF4J Update**
   - Upgrade from 1.7.35 to 2.0.x
   - Handle any API changes
   - Update logging configuration if needed

2. **SSHJ Update**
   - Upgrade to latest stable version
   - Test SSH connectivity functionality
   - Verify no breaking changes in API usage

### Phase 3: Testing Dependencies Update
1. **JUnit Migration** (if not done in test infrastructure plan)
   - Migrate from JUnit 4 to JUnit 5
   - Update test syntax and annotations
   - Configure new test runner

2. **Add Modern Testing Libraries**
   - Mockito for mocking
   - AssertJ for fluent assertions
   - TestContainers for integration testing

### Phase 4: Build and Plugin Dependencies
1. **Maven Plugins**
   - Update maven-compiler-plugin
   - Update maven-surefire-plugin
   - Update other Maven plugins to latest versions

2. **Build Tool Dependencies**
   - Update Maven wrapper if used
   - Ensure compatibility with updated plugins

## Technical Specifications

### Target Dependency Versions

#### Core Dependencies
```xml
<!-- SSH connectivity -->
<dependency>
    <groupId>com.hierynomus</groupId>
    <artifactId>sshj</artifactId>
    <version>0.37.0</version> <!-- Latest stable -->
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version> <!-- Latest 2.x -->
</dependency>
```

#### Test Dependencies
```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
    <scope>test</scope>
</dependency>

<!-- TestContainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

#### Build Plugins
```xml
<!-- Compiler -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
</plugin>

<!-- Surefire -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.2</version>
</plugin>

<!-- JAR -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.3.0</version>
</plugin>
```

### Breaking Changes Analysis

#### SLF4J 2.x Changes
- **Fluent API**: New fluent logging API available
- **Backward Compatibility**: 1.x API still supported
- **Performance**: Improved performance with lazy evaluation
- **Java Requirements**: Requires Java 8+ (compatible with our Java 17 target)

#### SSHJ Updates
- **API Stability**: Generally backward compatible
- **Security Improvements**: Enhanced security algorithms
- **Bug Fixes**: Various connectivity and stability fixes

## Migration Strategy

### Incremental Updates
1. **One Dependency at a Time**: Update dependencies individually
2. **Test After Each Update**: Run full test suite after each change
3. **Rollback Plan**: Keep previous versions documented for rollback

### Compatibility Testing
1. **Unit Tests**: Ensure all unit tests pass
2. **Integration Tests**: Verify real-world functionality
3. **Example Applications**: Test example code with new dependencies
4. **Performance Testing**: Ensure no performance regressions

## Risk Assessment

### Low Risk Updates
- **Patch Version Updates**: Bug fixes and security patches
- **Minor Version Updates**: New features with backward compatibility

### Medium Risk Updates
- **SLF4J 2.x**: Major version change but backward compatible
- **Build Plugin Updates**: May change build behavior

### High Risk Updates
- **JUnit 4 to 5**: Significant API changes (handled in test infrastructure plan)

## Acceptance Criteria

- [ ] All dependencies updated to target versions
- [ ] Security vulnerabilities resolved
- [ ] All tests pass with new dependencies
- [ ] No performance regressions
- [ ] Example applications work correctly
- [ ] Documentation updated with new requirements
- [ ] CI/CD pipeline works with new dependencies

## Testing Strategy

### Automated Testing
1. **Unit Test Suite**: Full execution with new dependencies
2. **Integration Tests**: Real TeamSpeak server connectivity
3. **Performance Tests**: Benchmark critical operations
4. **Security Scans**: Verify vulnerability resolution

### Manual Testing
1. **Example Applications**: Test all provided examples
2. **Edge Cases**: Test error conditions and edge cases
3. **Different Environments**: Test on various Java distributions

## Security Benefits

### Resolved Vulnerabilities
- Update to versions with known security fixes
- Remove dependencies with known CVEs
- Improve overall security posture

### Enhanced Security Features
- Modern cryptographic algorithms in SSHJ
- Improved logging security in SLF4J 2.x
- Better test isolation with modern testing frameworks

## Performance Expectations

### Expected Improvements
- **SLF4J 2.x**: 10-20% logging performance improvement
- **SSHJ**: Better connection handling and performance
- **JUnit 5**: Faster test execution with parallel testing

### Monitoring
- Benchmark before and after updates
- Monitor memory usage and CPU performance
- Track build and test execution times

## Documentation Updates

### README Changes
- Update dependency version requirements
- Update installation instructions
- Update compatibility matrix

### Changelog
- Document all dependency updates
- Note any breaking changes
- Highlight security improvements

## Success Metrics

- Zero security vulnerabilities in dependencies
- All tests passing with new versions
- Performance maintained or improved
- Successful CI/CD pipeline execution
- Positive developer feedback on new features

## Next Steps

After completion, this update enables:
- Improved security posture
- Access to modern library features
- Better performance and stability
- Foundation for future enhancements
- Reduced technical debt
