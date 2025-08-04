package com.github.theholywaffle.teamspeak3.api;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TextMessageTargetModeTest {

	@Test
	public void client_HasCorrectIndex() {
		assertEquals(1, TextMessageTargetMode.CLIENT.getIndex());
	}

	@Test
	public void channel_HasCorrectIndex() {
		assertEquals(2, TextMessageTargetMode.CHANNEL.getIndex());
	}

	@Test
	public void server_HasCorrectIndex() {
		assertEquals(3, TextMessageTargetMode.SERVER.getIndex());
	}

	@Test
	public void enumValues_ContainsAllExpectedValues() {
		TextMessageTargetMode[] values = TextMessageTargetMode.values();
		assertEquals(3, values.length);
		
		// Verify all expected values are present
		Set<TextMessageTargetMode> expectedValues = EnumSet.of(
			TextMessageTargetMode.CLIENT,
			TextMessageTargetMode.CHANNEL,
			TextMessageTargetMode.SERVER
		);
		
		for (TextMessageTargetMode mode : values) {
			assertTrue(expectedValues.contains(mode), "Unexpected enum value: " + mode);
		}
	}

	@Test
	public void valueOf_ReturnsCorrectEnum() {
		assertEquals(TextMessageTargetMode.CLIENT, TextMessageTargetMode.valueOf("CLIENT"));
		assertEquals(TextMessageTargetMode.CHANNEL, TextMessageTargetMode.valueOf("CHANNEL"));
		assertEquals(TextMessageTargetMode.SERVER, TextMessageTargetMode.valueOf("SERVER"));
	}

	@Test
	public void valueOf_ThrowsExceptionForInvalidName() {
		assertThrows(IllegalArgumentException.class, () -> TextMessageTargetMode.valueOf("INVALID_MODE"));
		assertThrows(IllegalArgumentException.class, () -> TextMessageTargetMode.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> TextMessageTargetMode.valueOf("client")); // lowercase
	}

	@Test
	public void valueOf_ThrowsExceptionForNullName() {
		assertThrows(NullPointerException.class, () -> TextMessageTargetMode.valueOf(null));
	}

	@Test
	public void toString_ReturnsEnumName() {
		assertEquals("CLIENT", TextMessageTargetMode.CLIENT.toString());
		assertEquals("CHANNEL", TextMessageTargetMode.CHANNEL.toString());
		assertEquals("SERVER", TextMessageTargetMode.SERVER.toString());
	}

	@Test
	public void name_ReturnsEnumName() {
		assertEquals("CLIENT", TextMessageTargetMode.CLIENT.name());
		assertEquals("CHANNEL", TextMessageTargetMode.CHANNEL.name());
		assertEquals("SERVER", TextMessageTargetMode.SERVER.name());
	}

	@Test
	public void ordinal_ReturnsCorrectOrder() {
		assertEquals(0, TextMessageTargetMode.CLIENT.ordinal());
		assertEquals(1, TextMessageTargetMode.CHANNEL.ordinal());
		assertEquals(2, TextMessageTargetMode.SERVER.ordinal());
	}

	@Test
	public void equals_WorksCorrectly() {
		assertEquals(TextMessageTargetMode.CLIENT, TextMessageTargetMode.CLIENT);
		assertEquals(TextMessageTargetMode.CHANNEL, TextMessageTargetMode.CHANNEL);
		assertEquals(TextMessageTargetMode.SERVER, TextMessageTargetMode.SERVER);
		assertNotEquals(TextMessageTargetMode.CLIENT, TextMessageTargetMode.CHANNEL);
		assertNotEquals(TextMessageTargetMode.CHANNEL, TextMessageTargetMode.SERVER);
		assertNotEquals(TextMessageTargetMode.CLIENT, TextMessageTargetMode.SERVER);
	}

	@Test
	public void hashCode_IsConsistent() {
		assertEquals(TextMessageTargetMode.CLIENT.hashCode(), TextMessageTargetMode.CLIENT.hashCode());
		assertEquals(TextMessageTargetMode.CHANNEL.hashCode(), TextMessageTargetMode.CHANNEL.hashCode());
		assertEquals(TextMessageTargetMode.SERVER.hashCode(), TextMessageTargetMode.SERVER.hashCode());
	}

	@Test
	public void compareTo_WorksCorrectly() {
		assertTrue(TextMessageTargetMode.CLIENT.compareTo(TextMessageTargetMode.CHANNEL) < 0);
		assertTrue(TextMessageTargetMode.CHANNEL.compareTo(TextMessageTargetMode.SERVER) < 0);
		assertTrue(TextMessageTargetMode.CLIENT.compareTo(TextMessageTargetMode.SERVER) < 0);
		
		assertTrue(TextMessageTargetMode.CHANNEL.compareTo(TextMessageTargetMode.CLIENT) > 0);
		assertTrue(TextMessageTargetMode.SERVER.compareTo(TextMessageTargetMode.CHANNEL) > 0);
		assertTrue(TextMessageTargetMode.SERVER.compareTo(TextMessageTargetMode.CLIENT) > 0);
		
		assertEquals(0, TextMessageTargetMode.CLIENT.compareTo(TextMessageTargetMode.CLIENT));
		assertEquals(0, TextMessageTargetMode.CHANNEL.compareTo(TextMessageTargetMode.CHANNEL));
		assertEquals(0, TextMessageTargetMode.SERVER.compareTo(TextMessageTargetMode.SERVER));
	}

	@Test
	public void getIndex_ReturnsUniqueValues() {
		// Verify that each enum has a unique index
		Set<Integer> indices = new java.util.HashSet<>();
		for (TextMessageTargetMode mode : TextMessageTargetMode.values()) {
			int index = mode.getIndex();
			assertFalse(indices.contains(index), "Duplicate index found: " + index);
			indices.add(index);
		}
		assertEquals(TextMessageTargetMode.values().length, indices.size(), "All indices should be unique");
	}

	@Test
	public void getIndex_ReturnsPositiveValues() {
		// Verify that all indices are positive (as they represent TeamSpeak target mode codes)
		for (TextMessageTargetMode mode : TextMessageTargetMode.values()) {
			assertTrue(mode.getIndex() > 0, "Index should be positive for " + mode);
		}
	}

	@Test
	public void textMessageTargetMode_TeamSpeakProtocolCompliance() {
		// Verify that the indices match TeamSpeak protocol specifications
		// According to TeamSpeak documentation:
		// - 1 = client (private message)
		// - 2 = channel message
		// - 3 = server message
		
		assertEquals(1, TextMessageTargetMode.CLIENT.getIndex(), 
			"CLIENT should have index 1 according to TeamSpeak protocol");
		assertEquals(2, TextMessageTargetMode.CHANNEL.getIndex(), 
			"CHANNEL should have index 2 according to TeamSpeak protocol");
		assertEquals(3, TextMessageTargetMode.SERVER.getIndex(), 
			"SERVER should have index 3 according to TeamSpeak protocol");
	}

	@Test
	public void textMessageTargetMode_EnumConstantsAreImmutable() {
		// Verify that enum constants are effectively immutable
		TextMessageTargetMode mode1 = TextMessageTargetMode.CLIENT;
		TextMessageTargetMode mode2 = TextMessageTargetMode.CLIENT;
		
		assertSame(mode1, mode2, "Enum constants should be singletons");
		assertEquals(mode1.getIndex(), mode2.getIndex(), "Index should be consistent");
	}

	@Test
	public void textMessageTargetMode_CanBeUsedInSwitchStatement() {
		// Verify that the enum can be used in switch statements
		for (TextMessageTargetMode mode : TextMessageTargetMode.values()) {
			String description;
			switch (mode) {
				case CLIENT:
					description = "Private message to a specific client";
					break;
				case CHANNEL:
					description = "Message to all clients in a channel";
					break;
				case SERVER:
					description = "Message to all clients on the server";
					break;
				default:
					description = "Unknown message target";
					break;
			}
			
			assertNotNull(description, "Switch statement should handle all enum values");
			assertFalse(description.isEmpty(), "Description should not be empty");
		}
	}

	@Test
	public void textMessageTargetMode_CanBeUsedInCollections() {
		// Verify that the enum can be used in collections
		Set<TextMessageTargetMode> modes = EnumSet.allOf(TextMessageTargetMode.class);
		assertEquals(3, modes.size(), "EnumSet should contain all enum values");
		
		assertTrue(modes.contains(TextMessageTargetMode.CLIENT));
		assertTrue(modes.contains(TextMessageTargetMode.CHANNEL));
		assertTrue(modes.contains(TextMessageTargetMode.SERVER));
	}

	@Test
	public void textMessageTargetMode_SerializationCompatible() {
		// Verify that enum values have consistent string representations for serialization
		for (TextMessageTargetMode mode : TextMessageTargetMode.values()) {
			String enumName = mode.name();
			assertNotNull(enumName, "Enum name should not be null");
			assertFalse(enumName.isEmpty(), "Enum name should not be empty");
			
			// Verify round-trip serialization
			TextMessageTargetMode deserialized = TextMessageTargetMode.valueOf(enumName);
			assertEquals(mode, deserialized, "Round-trip serialization should work for " + mode);
		}
	}

	@Test
	public void textMessageTargetMode_MessageScope() {
		// Test the logical scope of each message target mode
		
		// CLIENT has the narrowest scope (1-to-1)
		assertEquals(1, TextMessageTargetMode.CLIENT.getIndex(), 
			"CLIENT should have index 1 for private messaging");
		
		// CHANNEL has medium scope (1-to-many within channel)
		assertEquals(2, TextMessageTargetMode.CHANNEL.getIndex(), 
			"CHANNEL should have index 2 for channel-wide messaging");
		
		// SERVER has the broadest scope (1-to-all on server)
		assertEquals(3, TextMessageTargetMode.SERVER.getIndex(), 
			"SERVER should have index 3 for server-wide messaging");
		
		// Verify increasing scope order
		assertTrue(TextMessageTargetMode.CLIENT.getIndex() < TextMessageTargetMode.CHANNEL.getIndex(), 
			"CLIENT scope should be smaller than CHANNEL scope");
		assertTrue(TextMessageTargetMode.CHANNEL.getIndex() < TextMessageTargetMode.SERVER.getIndex(), 
			"CHANNEL scope should be smaller than SERVER scope");
	}

	@Test
	public void textMessageTargetMode_IndexOrderMatchesOrdinal() {
		// Verify that the index order follows a logical progression
		// Note: Index doesn't match ordinal in this case, but follows TeamSpeak protocol
		
		assertEquals(0, TextMessageTargetMode.CLIENT.ordinal());
		assertEquals(1, TextMessageTargetMode.CLIENT.getIndex());
		
		assertEquals(1, TextMessageTargetMode.CHANNEL.ordinal());
		assertEquals(2, TextMessageTargetMode.CHANNEL.getIndex());
		
		assertEquals(2, TextMessageTargetMode.SERVER.ordinal());
		assertEquals(3, TextMessageTargetMode.SERVER.getIndex());
		
		// Each index is ordinal + 1 (TeamSpeak protocol starts at 1, not 0)
		for (TextMessageTargetMode mode : TextMessageTargetMode.values()) {
			assertEquals(mode.ordinal() + 1, mode.getIndex(), 
				"Index should be ordinal + 1 for " + mode + " to match TeamSpeak protocol");
		}
	}

	@Test
	public void textMessageTargetMode_UsageScenarios() {
		// Test typical usage scenarios for each target mode
		
		// CLIENT mode for private messages
		TextMessageTargetMode clientMode = TextMessageTargetMode.CLIENT;
		assertEquals(1, clientMode.getIndex());
		assertEquals("CLIENT", clientMode.name());
		
		// CHANNEL mode for channel messages
		TextMessageTargetMode channelMode = TextMessageTargetMode.CHANNEL;
		assertEquals(2, channelMode.getIndex());
		assertEquals("CHANNEL", channelMode.name());
		
		// SERVER mode for server-wide announcements
		TextMessageTargetMode serverMode = TextMessageTargetMode.SERVER;
		assertEquals(3, serverMode.getIndex());
		assertEquals("SERVER", serverMode.name());
		
		// Verify they are all different
		assertNotEquals(clientMode, channelMode);
		assertNotEquals(channelMode, serverMode);
		assertNotEquals(clientMode, serverMode);
	}

	@Test
	public void textMessageTargetMode_EnumSetOperations() {
		// Test EnumSet operations
		EnumSet<TextMessageTargetMode> allModes = EnumSet.allOf(TextMessageTargetMode.class);
		assertEquals(3, allModes.size());
		
		EnumSet<TextMessageTargetMode> broadcastModes = EnumSet.of(
			TextMessageTargetMode.CHANNEL, 
			TextMessageTargetMode.SERVER
		);
		assertEquals(2, broadcastModes.size());
		assertFalse(broadcastModes.contains(TextMessageTargetMode.CLIENT));
		
		EnumSet<TextMessageTargetMode> privateOnly = EnumSet.of(TextMessageTargetMode.CLIENT);
		assertEquals(1, privateOnly.size());
		assertTrue(privateOnly.contains(TextMessageTargetMode.CLIENT));
	}
}
