package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VersionTest {

	@Test
	public void constructor_ValidMap() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.13.7");
		map.put("build", "1655727713");
		map.put("platform", "Linux");
		
		Version version = new Version(map);
		assertNotNull(version);
		assertEquals("3.13.7", version.getVersion());
		assertEquals("1655727713", version.getBuild());
		assertEquals("Linux", version.getPlatform());
	}

	@Test
	public void getVersion_ValidVersion() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.13.7");
		
		Version version = new Version(map);
		assertEquals("3.13.7", version.getVersion());
	}

	@Test
	public void getVersion_EmptyVersion() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "");
		
		Version version = new Version(map);
		assertEquals("", version.getVersion());
	}

	@Test
	public void getVersion_NullVersion() {
		Map<String, String> map = new HashMap<>();
		// version key not present
		
		Version version = new Version(map);
		assertEquals("", version.getVersion());
	}

	@Test
	public void getVersion_BetaVersion() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.14.0-beta.1");
		
		Version version = new Version(map);
		assertEquals("3.14.0-beta.1", version.getVersion());
	}

	@Test
	public void getVersion_AlphaVersion() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.15.0-alpha.2");
		
		Version version = new Version(map);
		assertEquals("3.15.0-alpha.2", version.getVersion());
	}

	@Test
	public void getBuild_ValidBuild() {
		Map<String, String> map = new HashMap<>();
		map.put("build", "1655727713");
		
		Version version = new Version(map);
		assertEquals("1655727713", version.getBuild());
	}

	@Test
	public void getBuild_EmptyBuild() {
		Map<String, String> map = new HashMap<>();
		map.put("build", "");
		
		Version version = new Version(map);
		assertEquals("", version.getBuild());
	}

	@Test
	public void getBuild_NullBuild() {
		Map<String, String> map = new HashMap<>();
		// build key not present
		
		Version version = new Version(map);
		assertEquals("", version.getBuild());
	}

	@Test
	public void getBuild_NumericBuild() {
		Map<String, String> map = new HashMap<>();
		map.put("build", "123456789");
		
		Version version = new Version(map);
		assertEquals("123456789", version.getBuild());
	}

	@Test
	public void getBuild_AlphanumericBuild() {
		Map<String, String> map = new HashMap<>();
		map.put("build", "build-abc123");
		
		Version version = new Version(map);
		assertEquals("build-abc123", version.getBuild());
	}

	@Test
	public void getPlatform_Linux() {
		Map<String, String> map = new HashMap<>();
		map.put("platform", "Linux");
		
		Version version = new Version(map);
		assertEquals("Linux", version.getPlatform());
	}

	@Test
	public void getPlatform_Windows() {
		Map<String, String> map = new HashMap<>();
		map.put("platform", "Windows");
		
		Version version = new Version(map);
		assertEquals("Windows", version.getPlatform());
	}

	@Test
	public void getPlatform_MacOS() {
		Map<String, String> map = new HashMap<>();
		map.put("platform", "macOS");
		
		Version version = new Version(map);
		assertEquals("macOS", version.getPlatform());
	}

	@Test
	public void getPlatform_FreeBSD() {
		Map<String, String> map = new HashMap<>();
		map.put("platform", "FreeBSD");
		
		Version version = new Version(map);
		assertEquals("FreeBSD", version.getPlatform());
	}

	@Test
	public void getPlatform_EmptyPlatform() {
		Map<String, String> map = new HashMap<>();
		map.put("platform", "");
		
		Version version = new Version(map);
		assertEquals("", version.getPlatform());
	}

	@Test
	public void getPlatform_NullPlatform() {
		Map<String, String> map = new HashMap<>();
		// platform key not present
		
		Version version = new Version(map);
		assertEquals("", version.getPlatform());
	}

	@Test
	public void version_CompleteData() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.13.7");
		map.put("build", "1655727713");
		map.put("platform", "Linux");
		
		Version version = new Version(map);
		
		assertEquals("3.13.7", version.getVersion());
		assertEquals("1655727713", version.getBuild());
		assertEquals("Linux", version.getPlatform());
	}

	@Test
	public void version_EmptyMap() {
		Map<String, String> map = new HashMap<>();
		
		Version version = new Version(map);
		
		assertEquals("", version.getVersion());
		assertEquals("", version.getBuild());
		assertEquals("", version.getPlatform());
	}

	@Test
	public void version_PartialData() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.12.1");
		// Missing build and platform
		
		Version version = new Version(map);
		
		assertEquals("3.12.1", version.getVersion());
		assertEquals("", version.getBuild()); // Default string value
		assertEquals("", version.getPlatform()); // Default string value
	}

	@Test
	public void version_InheritedWrapperMethods() {
		Map<String, String> map = new HashMap<>();
		map.put("version", "3.13.7");
		map.put("build", "1655727713");
		map.put("platform", "Linux");
		map.put("test_field", "test_value");
		
		Version version = new Version(map);
		
		// Test inherited methods from Wrapper
		assertEquals("3.13.7", version.get("version"));
		assertEquals("1655727713", version.get("build"));
		assertEquals("Linux", version.get("platform"));
		assertEquals("test_value", version.get("test_field"));
	}

	@Test
	public void version_RealWorldScenarios() {
		// Test various real-world version scenarios
		
		// Current stable version
		Map<String, String> stableMap = new HashMap<>();
		stableMap.put("version", "3.13.7");
		stableMap.put("build", "1655727713");
		stableMap.put("platform", "Linux");
		
		Version stableVersion = new Version(stableMap);
		assertEquals("3.13.7", stableVersion.getVersion());
		assertEquals("1655727713", stableVersion.getBuild());
		assertEquals("Linux", stableVersion.getPlatform());
		
		// Windows server version
		Map<String, String> windowsMap = new HashMap<>();
		windowsMap.put("version", "3.13.6");
		windowsMap.put("build", "1613181016");
		windowsMap.put("platform", "Windows");
		
		Version windowsVersion = new Version(windowsMap);
		assertEquals("3.13.6", windowsVersion.getVersion());
		assertEquals("1613181016", windowsVersion.getBuild());
		assertEquals("Windows", windowsVersion.getPlatform());
		
		// Beta version
		Map<String, String> betaMap = new HashMap<>();
		betaMap.put("version", "3.14.0-beta.1");
		betaMap.put("build", "1670000000");
		betaMap.put("platform", "Linux");
		
		Version betaVersion = new Version(betaMap);
		assertEquals("3.14.0-beta.1", betaVersion.getVersion());
		assertEquals("1670000000", betaVersion.getBuild());
		assertEquals("Linux", betaVersion.getPlatform());
	}

	@Test
	public void version_OlderVersions() {
		// Test older TeamSpeak versions
		
		// Older stable version
		Map<String, String> oldMap = new HashMap<>();
		oldMap.put("version", "3.12.1");
		oldMap.put("build", "1585896397");
		oldMap.put("platform", "Linux");
		
		Version oldVersion = new Version(oldMap);
		assertEquals("3.12.1", oldVersion.getVersion());
		assertEquals("1585896397", oldVersion.getBuild());
		assertEquals("Linux", oldVersion.getPlatform());
		
		// Very old version
		Map<String, String> veryOldMap = new HashMap<>();
		veryOldMap.put("version", "3.0.13.6");
		veryOldMap.put("build", "1478611251");
		veryOldMap.put("platform", "Linux");
		
		Version veryOldVersion = new Version(veryOldMap);
		assertEquals("3.0.13.6", veryOldVersion.getVersion());
		assertEquals("1478611251", veryOldVersion.getBuild());
		assertEquals("Linux", veryOldVersion.getPlatform());
	}

	@Test
	public void version_DifferentPlatforms() {
		// Test different platform variations
		
		// Linux 64-bit
		Map<String, String> linux64Map = new HashMap<>();
		linux64Map.put("version", "3.13.7");
		linux64Map.put("build", "1655727713");
		linux64Map.put("platform", "Linux");
		
		Version linux64Version = new Version(linux64Map);
		assertEquals("Linux", linux64Version.getPlatform());
		
		// Windows 64-bit
		Map<String, String> win64Map = new HashMap<>();
		win64Map.put("version", "3.13.7");
		win64Map.put("build", "1655727713");
		win64Map.put("platform", "Windows");
		
		Version win64Version = new Version(win64Map);
		assertEquals("Windows", win64Version.getPlatform());
		
		// FreeBSD
		Map<String, String> freebsdMap = new HashMap<>();
		freebsdMap.put("version", "3.13.7");
		freebsdMap.put("build", "1655727713");
		freebsdMap.put("platform", "FreeBSD");
		
		Version freebsdVersion = new Version(freebsdMap);
		assertEquals("FreeBSD", freebsdVersion.getPlatform());
	}

	@Test
	public void version_SpecialVersionFormats() {
		// Test special version formats
		
		// Release candidate
		Map<String, String> rcMap = new HashMap<>();
		rcMap.put("version", "3.14.0-rc.1");
		rcMap.put("build", "1670000001");
		rcMap.put("platform", "Linux");
		
		Version rcVersion = new Version(rcMap);
		assertEquals("3.14.0-rc.1", rcVersion.getVersion());
		assertEquals("1670000001", rcVersion.getBuild());
		assertEquals("Linux", rcVersion.getPlatform());
		
		// Snapshot version
		Map<String, String> snapshotMap = new HashMap<>();
		snapshotMap.put("version", "3.15.0-SNAPSHOT");
		snapshotMap.put("build", "dev-build-123");
		snapshotMap.put("platform", "Linux");
		
		Version snapshotVersion = new Version(snapshotMap);
		assertEquals("3.15.0-SNAPSHOT", snapshotVersion.getVersion());
		assertEquals("dev-build-123", snapshotVersion.getBuild());
		assertEquals("Linux", snapshotVersion.getPlatform());
	}

	@Test
	public void version_EdgeCases() {
		// Test edge cases
		
		// Very long version string
		Map<String, String> longMap = new HashMap<>();
		longMap.put("version", "3.13.7.1.2.3.4.5.6.7.8.9.10-very-long-version-string-with-many-parts");
		longMap.put("build", "very-long-build-identifier-with-many-characters-and-numbers-123456789");
		longMap.put("platform", "Very-Long-Platform-Name-With-Many-Words");
		
		Version longVersion = new Version(longMap);
		assertEquals("3.13.7.1.2.3.4.5.6.7.8.9.10-very-long-version-string-with-many-parts", longVersion.getVersion());
		assertEquals("very-long-build-identifier-with-many-characters-and-numbers-123456789", longVersion.getBuild());
		assertEquals("Very-Long-Platform-Name-With-Many-Words", longVersion.getPlatform());
		
		// Single character values
		Map<String, String> singleMap = new HashMap<>();
		singleMap.put("version", "1");
		singleMap.put("build", "2");
		singleMap.put("platform", "L");
		
		Version singleVersion = new Version(singleMap);
		assertEquals("1", singleVersion.getVersion());
		assertEquals("2", singleVersion.getBuild());
		assertEquals("L", singleVersion.getPlatform());
	}

	@Test
	public void version_SpecialCharacters() {
		// Test special characters in version info
		
		Map<String, String> specialMap = new HashMap<>();
		specialMap.put("version", "3.13.7 [Special Edition]");
		specialMap.put("build", "build-2023.06.20-final");
		specialMap.put("platform", "Linux (Ubuntu 20.04)");
		
		Version specialVersion = new Version(specialMap);
		assertEquals("3.13.7 [Special Edition]", specialVersion.getVersion());
		assertEquals("build-2023.06.20-final", specialVersion.getBuild());
		assertEquals("Linux (Ubuntu 20.04)", specialVersion.getPlatform());
	}

	@Test
	public void version_UnicodeCharacters() {
		// Test Unicode characters in version info
		
		Map<String, String> unicodeMap = new HashMap<>();
		unicodeMap.put("version", "3.13.7 ™");
		unicodeMap.put("build", "build-ñ123");
		unicodeMap.put("platform", "Lïñüx");
		
		Version unicodeVersion = new Version(unicodeMap);
		assertEquals("3.13.7 ™", unicodeVersion.getVersion());
		assertEquals("build-ñ123", unicodeVersion.getBuild());
		assertEquals("Lïñüx", unicodeVersion.getPlatform());
	}
}
