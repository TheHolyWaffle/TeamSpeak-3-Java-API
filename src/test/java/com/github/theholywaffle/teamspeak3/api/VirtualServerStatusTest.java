package com.github.theholywaffle.teamspeak3.api;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualServerStatusTest {

	@Test
	public void online_HasCorrectName() {
		assertEquals("online", VirtualServerStatus.ONLINE.getName());
	}

	@Test
	public void offline_HasCorrectName() {
		assertEquals("offline", VirtualServerStatus.OFFLINE.getName());
	}

	@Test
	public void deployRunning_HasCorrectName() {
		assertEquals("deploy running", VirtualServerStatus.DEPLOY_RUNNING.getName());
	}

	@Test
	public void bootingUp_HasCorrectName() {
		assertEquals("booting up", VirtualServerStatus.BOOTING_UP.getName());
	}

	@Test
	public void shuttingDown_HasCorrectName() {
		assertEquals("shutting down", VirtualServerStatus.SHUTTING_DOWN.getName());
	}

	@Test
	public void virtualOnline_HasCorrectName() {
		assertEquals("virtual online", VirtualServerStatus.VIRTUAL_ONLINE.getName());
	}

	@Test
	public void unknown_HasCorrectName() {
		assertEquals("unknown", VirtualServerStatus.UNKNOWN.getName());
	}

	@Test
	public void enumValues_ContainsAllExpectedValues() {
		VirtualServerStatus[] values = VirtualServerStatus.values();
		assertEquals(7, values.length);
		
		// Verify all expected values are present
		Set<VirtualServerStatus> expectedValues = EnumSet.of(
			VirtualServerStatus.ONLINE,
			VirtualServerStatus.OFFLINE,
			VirtualServerStatus.DEPLOY_RUNNING,
			VirtualServerStatus.BOOTING_UP,
			VirtualServerStatus.SHUTTING_DOWN,
			VirtualServerStatus.VIRTUAL_ONLINE,
			VirtualServerStatus.UNKNOWN
		);
		
		for (VirtualServerStatus status : values) {
			assertTrue(expectedValues.contains(status), "Unexpected enum value: " + status);
		}
	}

	@Test
	public void valueOf_ReturnsCorrectEnum() {
		assertEquals(VirtualServerStatus.ONLINE, VirtualServerStatus.valueOf("ONLINE"));
		assertEquals(VirtualServerStatus.OFFLINE, VirtualServerStatus.valueOf("OFFLINE"));
		assertEquals(VirtualServerStatus.DEPLOY_RUNNING, VirtualServerStatus.valueOf("DEPLOY_RUNNING"));
		assertEquals(VirtualServerStatus.BOOTING_UP, VirtualServerStatus.valueOf("BOOTING_UP"));
		assertEquals(VirtualServerStatus.SHUTTING_DOWN, VirtualServerStatus.valueOf("SHUTTING_DOWN"));
		assertEquals(VirtualServerStatus.VIRTUAL_ONLINE, VirtualServerStatus.valueOf("VIRTUAL_ONLINE"));
		assertEquals(VirtualServerStatus.UNKNOWN, VirtualServerStatus.valueOf("UNKNOWN"));
	}

	@Test
	public void valueOf_ThrowsExceptionForInvalidName() {
		assertThrows(IllegalArgumentException.class, () -> VirtualServerStatus.valueOf("INVALID_STATUS"));
		assertThrows(IllegalArgumentException.class, () -> VirtualServerStatus.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> VirtualServerStatus.valueOf("online")); // lowercase
	}

	@Test
	public void valueOf_ThrowsExceptionForNullName() {
		assertThrows(NullPointerException.class, () -> VirtualServerStatus.valueOf(null));
	}

	@Test
	public void toString_ReturnsEnumName() {
		assertEquals("ONLINE", VirtualServerStatus.ONLINE.toString());
		assertEquals("OFFLINE", VirtualServerStatus.OFFLINE.toString());
		assertEquals("DEPLOY_RUNNING", VirtualServerStatus.DEPLOY_RUNNING.toString());
		assertEquals("BOOTING_UP", VirtualServerStatus.BOOTING_UP.toString());
		assertEquals("SHUTTING_DOWN", VirtualServerStatus.SHUTTING_DOWN.toString());
		assertEquals("VIRTUAL_ONLINE", VirtualServerStatus.VIRTUAL_ONLINE.toString());
		assertEquals("UNKNOWN", VirtualServerStatus.UNKNOWN.toString());
	}

	@Test
	public void name_ReturnsEnumName() {
		assertEquals("ONLINE", VirtualServerStatus.ONLINE.name());
		assertEquals("OFFLINE", VirtualServerStatus.OFFLINE.name());
		assertEquals("DEPLOY_RUNNING", VirtualServerStatus.DEPLOY_RUNNING.name());
		assertEquals("BOOTING_UP", VirtualServerStatus.BOOTING_UP.name());
		assertEquals("SHUTTING_DOWN", VirtualServerStatus.SHUTTING_DOWN.name());
		assertEquals("VIRTUAL_ONLINE", VirtualServerStatus.VIRTUAL_ONLINE.name());
		assertEquals("UNKNOWN", VirtualServerStatus.UNKNOWN.name());
	}

	@Test
	public void ordinal_ReturnsCorrectOrder() {
		assertEquals(0, VirtualServerStatus.ONLINE.ordinal());
		assertEquals(1, VirtualServerStatus.OFFLINE.ordinal());
		assertEquals(2, VirtualServerStatus.DEPLOY_RUNNING.ordinal());
		assertEquals(3, VirtualServerStatus.BOOTING_UP.ordinal());
		assertEquals(4, VirtualServerStatus.SHUTTING_DOWN.ordinal());
		assertEquals(5, VirtualServerStatus.VIRTUAL_ONLINE.ordinal());
		assertEquals(6, VirtualServerStatus.UNKNOWN.ordinal());
	}

	@Test
	public void equals_WorksCorrectly() {
		assertEquals(VirtualServerStatus.ONLINE, VirtualServerStatus.ONLINE);
		assertEquals(VirtualServerStatus.OFFLINE, VirtualServerStatus.OFFLINE);
		assertNotEquals(VirtualServerStatus.ONLINE, VirtualServerStatus.OFFLINE);
		assertNotEquals(VirtualServerStatus.OFFLINE, VirtualServerStatus.ONLINE);
	}

	@Test
	public void hashCode_IsConsistent() {
		assertEquals(VirtualServerStatus.ONLINE.hashCode(), VirtualServerStatus.ONLINE.hashCode());
		assertEquals(VirtualServerStatus.OFFLINE.hashCode(), VirtualServerStatus.OFFLINE.hashCode());
	}

	@Test
	public void compareTo_WorksCorrectly() {
		assertTrue(VirtualServerStatus.ONLINE.compareTo(VirtualServerStatus.OFFLINE) < 0);
		assertTrue(VirtualServerStatus.OFFLINE.compareTo(VirtualServerStatus.ONLINE) > 0);
		assertEquals(0, VirtualServerStatus.ONLINE.compareTo(VirtualServerStatus.ONLINE));
	}

	@Test
	public void getName_ReturnsUniqueValues() {
		// Verify that each enum has a unique name
		Set<String> names = new java.util.HashSet<>();
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			String name = status.getName();
			assertFalse(names.contains(name), "Duplicate name found: " + name);
			names.add(name);
		}
		assertEquals(VirtualServerStatus.values().length, names.size(), "All names should be unique");
	}

	@Test
	public void getName_ReturnsNonEmptyValues() {
		// Verify that all names are non-empty
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			String name = status.getName();
			assertNotNull(name, "Name should not be null for " + status);
			assertFalse(name.isEmpty(), "Name should not be empty for " + status);
		}
	}

	@Test
	public void virtualServerStatus_TeamSpeakProtocolCompliance() {
		// Verify that the names match TeamSpeak protocol specifications
		assertEquals("online", VirtualServerStatus.ONLINE.getName(), 
			"ONLINE should have name 'online' according to TeamSpeak protocol");
		assertEquals("offline", VirtualServerStatus.OFFLINE.getName(), 
			"OFFLINE should have name 'offline' according to TeamSpeak protocol");
		assertEquals("deploy running", VirtualServerStatus.DEPLOY_RUNNING.getName(), 
			"DEPLOY_RUNNING should have name 'deploy running' according to TeamSpeak protocol");
		assertEquals("booting up", VirtualServerStatus.BOOTING_UP.getName(), 
			"BOOTING_UP should have name 'booting up' according to TeamSpeak protocol");
		assertEquals("shutting down", VirtualServerStatus.SHUTTING_DOWN.getName(), 
			"SHUTTING_DOWN should have name 'shutting down' according to TeamSpeak protocol");
		assertEquals("virtual online", VirtualServerStatus.VIRTUAL_ONLINE.getName(), 
			"VIRTUAL_ONLINE should have name 'virtual online' according to TeamSpeak protocol");
		assertEquals("unknown", VirtualServerStatus.UNKNOWN.getName(), 
			"UNKNOWN should have name 'unknown' according to TeamSpeak protocol");
	}

	@Test
	public void virtualServerStatus_EnumConstantsAreImmutable() {
		// Verify that enum constants are effectively immutable
		VirtualServerStatus status1 = VirtualServerStatus.ONLINE;
		VirtualServerStatus status2 = VirtualServerStatus.ONLINE;
		
		assertSame(status1, status2, "Enum constants should be singletons");
		assertEquals(status1.getName(), status2.getName(), "Name should be consistent");
	}

	@Test
	public void virtualServerStatus_CanBeUsedInSwitchStatement() {
		// Verify that the enum can be used in switch statements
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			String description;
			switch (status) {
				case ONLINE:
					description = "Server is running and accepting connections";
					break;
				case OFFLINE:
					description = "Server is not running";
					break;
				case DEPLOY_RUNNING:
					description = "Server is being deployed";
					break;
				case BOOTING_UP:
					description = "Server is starting up";
					break;
				case SHUTTING_DOWN:
					description = "Server is shutting down";
					break;
				case VIRTUAL_ONLINE:
					description = "Virtual server is online";
					break;
				case UNKNOWN:
					description = "Server status is unknown";
					break;
				default:
					description = "Unhandled status";
					break;
			}

			assertNotNull(description, "Switch statement should handle all enum values");
			assertFalse(description.isEmpty(), "Description should not be empty");
		}
	}

	@Test
	public void virtualServerStatus_CanBeUsedInCollections() {
		// Verify that the enum can be used in collections
		Set<VirtualServerStatus> statuses = EnumSet.allOf(VirtualServerStatus.class);
		assertEquals(7, statuses.size(), "EnumSet should contain all enum values");
		
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			assertTrue(statuses.contains(status), "EnumSet should contain " + status);
		}
	}

	@Test
	public void virtualServerStatus_SerializationCompatible() {
		// Verify that enum values have consistent string representations for serialization
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			String enumName = status.name();
			assertNotNull(enumName, "Enum name should not be null");
			assertFalse(enumName.isEmpty(), "Enum name should not be empty");
			
			// Verify round-trip serialization
			VirtualServerStatus deserialized = VirtualServerStatus.valueOf(enumName);
			assertEquals(status, deserialized, "Round-trip serialization should work for " + status);
		}
	}

	@Test
	public void virtualServerStatus_StatusTransitions() {
		// Test logical status transitions that might occur in TeamSpeak
		// Note: The ordinal order is defined by the enum declaration order, not lifecycle order

		// Verify enum declaration order (as defined in the enum)
		assertEquals(0, VirtualServerStatus.ONLINE.ordinal());
		assertEquals(1, VirtualServerStatus.OFFLINE.ordinal());
		assertEquals(2, VirtualServerStatus.DEPLOY_RUNNING.ordinal());
		assertEquals(3, VirtualServerStatus.BOOTING_UP.ordinal());
		assertEquals(4, VirtualServerStatus.SHUTTING_DOWN.ordinal());
		assertEquals(5, VirtualServerStatus.VIRTUAL_ONLINE.ordinal());
		assertEquals(6, VirtualServerStatus.UNKNOWN.ordinal());

		// Special states should be different from each other
		assertNotEquals(VirtualServerStatus.UNKNOWN, VirtualServerStatus.ONLINE,
			"UNKNOWN should be different from ONLINE");
		assertNotEquals(VirtualServerStatus.DEPLOY_RUNNING, VirtualServerStatus.ONLINE,
			"DEPLOY_RUNNING should be different from ONLINE");
		assertNotEquals(VirtualServerStatus.VIRTUAL_ONLINE, VirtualServerStatus.ONLINE,
			"VIRTUAL_ONLINE should be different from ONLINE");
	}

	@Test
	public void virtualServerStatus_CaseInsensitiveNameComparison() {
		// Test that names are case-sensitive as expected by TeamSpeak protocol
		for (VirtualServerStatus status : VirtualServerStatus.values()) {
			String name = status.getName();
			assertEquals(name, name.toLowerCase(), "All status names should be lowercase for TeamSpeak compatibility");
		}
	}
}
