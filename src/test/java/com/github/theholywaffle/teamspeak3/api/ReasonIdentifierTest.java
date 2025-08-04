package com.github.theholywaffle.teamspeak3.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReasonIdentifierTest {

	@Test
	public void reasonKickChannel_HasCorrectIndex() {
		assertEquals(4, ReasonIdentifier.REASON_KICK_CHANNEL.getIndex());
	}

	@Test
	public void reasonKickServer_HasCorrectIndex() {
		assertEquals(5, ReasonIdentifier.REASON_KICK_SERVER.getIndex());
	}

	@Test
	public void enumValues_ContainsAllExpectedValues() {
		ReasonIdentifier[] values = ReasonIdentifier.values();
		assertEquals(2, values.length);
		
		// Verify all expected values are present
		boolean hasKickChannel = false;
		boolean hasKickServer = false;
		
		for (ReasonIdentifier reason : values) {
			if (reason == ReasonIdentifier.REASON_KICK_CHANNEL) {
				hasKickChannel = true;
			} else if (reason == ReasonIdentifier.REASON_KICK_SERVER) {
				hasKickServer = true;
			}
		}
		
		assertTrue(hasKickChannel, "REASON_KICK_CHANNEL should be present");
		assertTrue(hasKickServer, "REASON_KICK_SERVER should be present");
	}

	@Test
	public void valueOf_ReturnsCorrectEnum() {
		assertEquals(ReasonIdentifier.REASON_KICK_CHANNEL, ReasonIdentifier.valueOf("REASON_KICK_CHANNEL"));
		assertEquals(ReasonIdentifier.REASON_KICK_SERVER, ReasonIdentifier.valueOf("REASON_KICK_SERVER"));
	}

	@Test
	public void valueOf_ThrowsExceptionForInvalidName() {
		assertThrows(IllegalArgumentException.class, () -> ReasonIdentifier.valueOf("INVALID_REASON"));
		assertThrows(IllegalArgumentException.class, () -> ReasonIdentifier.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> ReasonIdentifier.valueOf("reason_kick_channel")); // lowercase
	}

	@Test
	public void valueOf_ThrowsExceptionForNullName() {
		assertThrows(NullPointerException.class, () -> ReasonIdentifier.valueOf(null));
	}

	@Test
	public void toString_ReturnsEnumName() {
		assertEquals("REASON_KICK_CHANNEL", ReasonIdentifier.REASON_KICK_CHANNEL.toString());
		assertEquals("REASON_KICK_SERVER", ReasonIdentifier.REASON_KICK_SERVER.toString());
	}

	@Test
	public void name_ReturnsEnumName() {
		assertEquals("REASON_KICK_CHANNEL", ReasonIdentifier.REASON_KICK_CHANNEL.name());
		assertEquals("REASON_KICK_SERVER", ReasonIdentifier.REASON_KICK_SERVER.name());
	}

	@Test
	public void ordinal_ReturnsCorrectOrder() {
		assertEquals(0, ReasonIdentifier.REASON_KICK_CHANNEL.ordinal());
		assertEquals(1, ReasonIdentifier.REASON_KICK_SERVER.ordinal());
	}

	@Test
	public void equals_WorksCorrectly() {
		assertEquals(ReasonIdentifier.REASON_KICK_CHANNEL, ReasonIdentifier.REASON_KICK_CHANNEL);
		assertEquals(ReasonIdentifier.REASON_KICK_SERVER, ReasonIdentifier.REASON_KICK_SERVER);
		assertNotEquals(ReasonIdentifier.REASON_KICK_CHANNEL, ReasonIdentifier.REASON_KICK_SERVER);
		assertNotEquals(ReasonIdentifier.REASON_KICK_SERVER, ReasonIdentifier.REASON_KICK_CHANNEL);
	}

	@Test
	public void hashCode_IsConsistent() {
		assertEquals(ReasonIdentifier.REASON_KICK_CHANNEL.hashCode(), ReasonIdentifier.REASON_KICK_CHANNEL.hashCode());
		assertEquals(ReasonIdentifier.REASON_KICK_SERVER.hashCode(), ReasonIdentifier.REASON_KICK_SERVER.hashCode());
	}

	@Test
	public void compareTo_WorksCorrectly() {
		assertTrue(ReasonIdentifier.REASON_KICK_CHANNEL.compareTo(ReasonIdentifier.REASON_KICK_SERVER) < 0);
		assertTrue(ReasonIdentifier.REASON_KICK_SERVER.compareTo(ReasonIdentifier.REASON_KICK_CHANNEL) > 0);
		assertEquals(0, ReasonIdentifier.REASON_KICK_CHANNEL.compareTo(ReasonIdentifier.REASON_KICK_CHANNEL));
		assertEquals(0, ReasonIdentifier.REASON_KICK_SERVER.compareTo(ReasonIdentifier.REASON_KICK_SERVER));
	}

	@Test
	public void getIndex_ReturnsUniqueValues() {
		// Verify that each enum has a unique index
		int kickChannelIndex = ReasonIdentifier.REASON_KICK_CHANNEL.getIndex();
		int kickServerIndex = ReasonIdentifier.REASON_KICK_SERVER.getIndex();
		
		assertNotEquals(kickChannelIndex, kickServerIndex, "Each reason should have a unique index");
	}

	@Test
	public void getIndex_ReturnsPositiveValues() {
		// Verify that all indices are positive (as they represent TeamSpeak reason codes)
		assertTrue(ReasonIdentifier.REASON_KICK_CHANNEL.getIndex() > 0, "Kick channel index should be positive");
		assertTrue(ReasonIdentifier.REASON_KICK_SERVER.getIndex() > 0, "Kick server index should be positive");
	}

	@Test
	public void reasonIdentifier_TeamSpeakProtocolCompliance() {
		// Verify that the indices match TeamSpeak protocol specifications
		// According to TeamSpeak documentation:
		// - 4 = kick from channel
		// - 5 = kick from server
		
		assertEquals(4, ReasonIdentifier.REASON_KICK_CHANNEL.getIndex(), 
			"REASON_KICK_CHANNEL should have index 4 according to TeamSpeak protocol");
		assertEquals(5, ReasonIdentifier.REASON_KICK_SERVER.getIndex(), 
			"REASON_KICK_SERVER should have index 5 according to TeamSpeak protocol");
	}

	@Test
	public void reasonIdentifier_EnumConstantsAreImmutable() {
		// Verify that enum constants are effectively immutable
		ReasonIdentifier reason1 = ReasonIdentifier.REASON_KICK_CHANNEL;
		ReasonIdentifier reason2 = ReasonIdentifier.REASON_KICK_CHANNEL;
		
		assertSame(reason1, reason2, "Enum constants should be singletons");
		assertEquals(reason1.getIndex(), reason2.getIndex(), "Index should be consistent");
	}

	@Test
	public void reasonIdentifier_CanBeUsedInSwitchStatement() {
		// Verify that the enum can be used in switch statements
		for (ReasonIdentifier reason : ReasonIdentifier.values()) {
			String description;
			switch (reason) {
				case REASON_KICK_CHANNEL:
					description = "Kicked from channel";
					break;
				case REASON_KICK_SERVER:
					description = "Kicked from server";
					break;
				default:
					description = "Unknown reason";
					break;
			}

			assertNotNull(description, "Switch statement should handle all enum values");
			assertFalse(description.isEmpty(), "Description should not be empty");
		}
	}

	@Test
	public void reasonIdentifier_CanBeUsedInCollections() {
		// Verify that the enum can be used in collections
		java.util.Set<ReasonIdentifier> reasons = java.util.EnumSet.allOf(ReasonIdentifier.class);
		assertEquals(2, reasons.size(), "EnumSet should contain all enum values");
		assertTrue(reasons.contains(ReasonIdentifier.REASON_KICK_CHANNEL));
		assertTrue(reasons.contains(ReasonIdentifier.REASON_KICK_SERVER));
	}

	@Test
	public void reasonIdentifier_SerializationCompatible() {
		// Verify that enum values have consistent string representations for serialization
		assertEquals("REASON_KICK_CHANNEL", ReasonIdentifier.REASON_KICK_CHANNEL.name());
		assertEquals("REASON_KICK_SERVER", ReasonIdentifier.REASON_KICK_SERVER.name());
		
		// Verify round-trip serialization
		String serialized = ReasonIdentifier.REASON_KICK_CHANNEL.name();
		ReasonIdentifier deserialized = ReasonIdentifier.valueOf(serialized);
		assertEquals(ReasonIdentifier.REASON_KICK_CHANNEL, deserialized);
	}
}
