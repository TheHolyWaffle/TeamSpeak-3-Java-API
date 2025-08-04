package com.github.theholywaffle.teamspeak3.api;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionGroupDatabaseTypeTest {

	@Test
	public void template_HasCorrectIndex() {
		assertEquals(0, PermissionGroupDatabaseType.TEMPLATE.getIndex());
	}

	@Test
	public void regular_HasCorrectIndex() {
		assertEquals(1, PermissionGroupDatabaseType.REGULAR.getIndex());
	}

	@Test
	public void query_HasCorrectIndex() {
		assertEquals(2, PermissionGroupDatabaseType.QUERY.getIndex());
	}

	@Test
	public void enumValues_ContainsAllExpectedValues() {
		PermissionGroupDatabaseType[] values = PermissionGroupDatabaseType.values();
		assertEquals(3, values.length);
		
		// Verify all expected values are present
		Set<PermissionGroupDatabaseType> expectedValues = EnumSet.of(
			PermissionGroupDatabaseType.TEMPLATE,
			PermissionGroupDatabaseType.REGULAR,
			PermissionGroupDatabaseType.QUERY
		);
		
		for (PermissionGroupDatabaseType type : values) {
			assertTrue(expectedValues.contains(type), "Unexpected enum value: " + type);
		}
	}

	@Test
	public void valueOf_ReturnsCorrectEnum() {
		assertEquals(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.valueOf("TEMPLATE"));
		assertEquals(PermissionGroupDatabaseType.REGULAR, PermissionGroupDatabaseType.valueOf("REGULAR"));
		assertEquals(PermissionGroupDatabaseType.QUERY, PermissionGroupDatabaseType.valueOf("QUERY"));
	}

	@Test
	public void valueOf_ThrowsExceptionForInvalidName() {
		assertThrows(IllegalArgumentException.class, () -> PermissionGroupDatabaseType.valueOf("INVALID_TYPE"));
		assertThrows(IllegalArgumentException.class, () -> PermissionGroupDatabaseType.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> PermissionGroupDatabaseType.valueOf("template")); // lowercase
	}

	@Test
	public void valueOf_ThrowsExceptionForNullName() {
		assertThrows(NullPointerException.class, () -> PermissionGroupDatabaseType.valueOf(null));
	}

	@Test
	public void toString_ReturnsEnumName() {
		assertEquals("TEMPLATE", PermissionGroupDatabaseType.TEMPLATE.toString());
		assertEquals("REGULAR", PermissionGroupDatabaseType.REGULAR.toString());
		assertEquals("QUERY", PermissionGroupDatabaseType.QUERY.toString());
	}

	@Test
	public void name_ReturnsEnumName() {
		assertEquals("TEMPLATE", PermissionGroupDatabaseType.TEMPLATE.name());
		assertEquals("REGULAR", PermissionGroupDatabaseType.REGULAR.name());
		assertEquals("QUERY", PermissionGroupDatabaseType.QUERY.name());
	}

	@Test
	public void ordinal_ReturnsCorrectOrder() {
		assertEquals(0, PermissionGroupDatabaseType.TEMPLATE.ordinal());
		assertEquals(1, PermissionGroupDatabaseType.REGULAR.ordinal());
		assertEquals(2, PermissionGroupDatabaseType.QUERY.ordinal());
	}

	@Test
	public void equals_WorksCorrectly() {
		assertEquals(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.TEMPLATE);
		assertEquals(PermissionGroupDatabaseType.REGULAR, PermissionGroupDatabaseType.REGULAR);
		assertEquals(PermissionGroupDatabaseType.QUERY, PermissionGroupDatabaseType.QUERY);
		assertNotEquals(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.REGULAR);
		assertNotEquals(PermissionGroupDatabaseType.REGULAR, PermissionGroupDatabaseType.QUERY);
		assertNotEquals(PermissionGroupDatabaseType.TEMPLATE, PermissionGroupDatabaseType.QUERY);
	}

	@Test
	public void hashCode_IsConsistent() {
		assertEquals(PermissionGroupDatabaseType.TEMPLATE.hashCode(), PermissionGroupDatabaseType.TEMPLATE.hashCode());
		assertEquals(PermissionGroupDatabaseType.REGULAR.hashCode(), PermissionGroupDatabaseType.REGULAR.hashCode());
		assertEquals(PermissionGroupDatabaseType.QUERY.hashCode(), PermissionGroupDatabaseType.QUERY.hashCode());
	}

	@Test
	public void compareTo_WorksCorrectly() {
		assertTrue(PermissionGroupDatabaseType.TEMPLATE.compareTo(PermissionGroupDatabaseType.REGULAR) < 0);
		assertTrue(PermissionGroupDatabaseType.REGULAR.compareTo(PermissionGroupDatabaseType.QUERY) < 0);
		assertTrue(PermissionGroupDatabaseType.TEMPLATE.compareTo(PermissionGroupDatabaseType.QUERY) < 0);
		
		assertTrue(PermissionGroupDatabaseType.REGULAR.compareTo(PermissionGroupDatabaseType.TEMPLATE) > 0);
		assertTrue(PermissionGroupDatabaseType.QUERY.compareTo(PermissionGroupDatabaseType.REGULAR) > 0);
		assertTrue(PermissionGroupDatabaseType.QUERY.compareTo(PermissionGroupDatabaseType.TEMPLATE) > 0);
		
		assertEquals(0, PermissionGroupDatabaseType.TEMPLATE.compareTo(PermissionGroupDatabaseType.TEMPLATE));
		assertEquals(0, PermissionGroupDatabaseType.REGULAR.compareTo(PermissionGroupDatabaseType.REGULAR));
		assertEquals(0, PermissionGroupDatabaseType.QUERY.compareTo(PermissionGroupDatabaseType.QUERY));
	}

	@Test
	public void getIndex_ReturnsUniqueValues() {
		// Verify that each enum has a unique index
		Set<Integer> indices = new java.util.HashSet<>();
		for (PermissionGroupDatabaseType type : PermissionGroupDatabaseType.values()) {
			int index = type.getIndex();
			assertFalse(indices.contains(index), "Duplicate index found: " + index);
			indices.add(index);
		}
		assertEquals(PermissionGroupDatabaseType.values().length, indices.size(), "All indices should be unique");
	}

	@Test
	public void getIndex_ReturnsNonNegativeValues() {
		// Verify that all indices are non-negative (as they represent TeamSpeak type codes)
		for (PermissionGroupDatabaseType type : PermissionGroupDatabaseType.values()) {
			assertTrue(type.getIndex() >= 0, "Index should be non-negative for " + type);
		}
	}

	@Test
	public void permissionGroupDatabaseType_TeamSpeakProtocolCompliance() {
		// Verify that the indices match TeamSpeak protocol specifications
		// According to TeamSpeak documentation:
		// - 0 = template group
		// - 1 = regular group
		// - 2 = query group
		
		assertEquals(0, PermissionGroupDatabaseType.TEMPLATE.getIndex(), 
			"TEMPLATE should have index 0 according to TeamSpeak protocol");
		assertEquals(1, PermissionGroupDatabaseType.REGULAR.getIndex(), 
			"REGULAR should have index 1 according to TeamSpeak protocol");
		assertEquals(2, PermissionGroupDatabaseType.QUERY.getIndex(), 
			"QUERY should have index 2 according to TeamSpeak protocol");
	}

	@Test
	public void permissionGroupDatabaseType_EnumConstantsAreImmutable() {
		// Verify that enum constants are effectively immutable
		PermissionGroupDatabaseType type1 = PermissionGroupDatabaseType.REGULAR;
		PermissionGroupDatabaseType type2 = PermissionGroupDatabaseType.REGULAR;
		
		assertSame(type1, type2, "Enum constants should be singletons");
		assertEquals(type1.getIndex(), type2.getIndex(), "Index should be consistent");
	}

	@Test
	public void permissionGroupDatabaseType_CanBeUsedInSwitchStatement() {
		// Verify that the enum can be used in switch statements
		for (PermissionGroupDatabaseType type : PermissionGroupDatabaseType.values()) {
			String description;
			switch (type) {
				case TEMPLATE:
					description = "Template group for creating other groups";
					break;
				case REGULAR:
					description = "Regular permission group";
					break;
				case QUERY:
					description = "ServerQuery permission group";
					break;
				default:
					description = "Unknown group type";
					break;
			}

			assertNotNull(description, "Switch statement should handle all enum values");
			assertFalse(description.isEmpty(), "Description should not be empty");
		}
	}

	@Test
	public void permissionGroupDatabaseType_CanBeUsedInCollections() {
		// Verify that the enum can be used in collections
		Set<PermissionGroupDatabaseType> types = EnumSet.allOf(PermissionGroupDatabaseType.class);
		assertEquals(3, types.size(), "EnumSet should contain all enum values");
		
		assertTrue(types.contains(PermissionGroupDatabaseType.TEMPLATE));
		assertTrue(types.contains(PermissionGroupDatabaseType.REGULAR));
		assertTrue(types.contains(PermissionGroupDatabaseType.QUERY));
	}

	@Test
	public void permissionGroupDatabaseType_SerializationCompatible() {
		// Verify that enum values have consistent string representations for serialization
		for (PermissionGroupDatabaseType type : PermissionGroupDatabaseType.values()) {
			String enumName = type.name();
			assertNotNull(enumName, "Enum name should not be null");
			assertFalse(enumName.isEmpty(), "Enum name should not be empty");
			
			// Verify round-trip serialization
			PermissionGroupDatabaseType deserialized = PermissionGroupDatabaseType.valueOf(enumName);
			assertEquals(type, deserialized, "Round-trip serialization should work for " + type);
		}
	}

	@Test
	public void permissionGroupDatabaseType_LogicalHierarchy() {
		// Test logical hierarchy of permission group types
		
		// Template groups are typically used as base templates (index 0)
		assertEquals(0, PermissionGroupDatabaseType.TEMPLATE.getIndex(), 
			"TEMPLATE should have the lowest index as it's the base type");
		
		// Regular groups are the most common type (index 1)
		assertEquals(1, PermissionGroupDatabaseType.REGULAR.getIndex(), 
			"REGULAR should have index 1 as it's the standard type");
		
		// Query groups are specialized for ServerQuery access (index 2)
		assertEquals(2, PermissionGroupDatabaseType.QUERY.getIndex(), 
			"QUERY should have index 2 as it's a specialized type");
	}

	@Test
	public void permissionGroupDatabaseType_IndexOrderMatchesOrdinal() {
		// Verify that the index order matches the ordinal order
		for (PermissionGroupDatabaseType type : PermissionGroupDatabaseType.values()) {
			assertEquals(type.ordinal(), type.getIndex(), 
				"Index should match ordinal for " + type + " to maintain consistency");
		}
	}

	@Test
	public void permissionGroupDatabaseType_UsageScenarios() {
		// Test typical usage scenarios for each type
		
		// Template groups are used for creating other groups
		PermissionGroupDatabaseType templateType = PermissionGroupDatabaseType.TEMPLATE;
		assertEquals(0, templateType.getIndex());
		assertEquals("TEMPLATE", templateType.name());
		
		// Regular groups are used for normal users
		PermissionGroupDatabaseType regularType = PermissionGroupDatabaseType.REGULAR;
		assertEquals(1, regularType.getIndex());
		assertEquals("REGULAR", regularType.name());
		
		// Query groups are used for ServerQuery connections
		PermissionGroupDatabaseType queryType = PermissionGroupDatabaseType.QUERY;
		assertEquals(2, queryType.getIndex());
		assertEquals("QUERY", queryType.name());
		
		// Verify they are all different
		assertNotEquals(templateType, regularType);
		assertNotEquals(regularType, queryType);
		assertNotEquals(templateType, queryType);
	}

	@Test
	public void permissionGroupDatabaseType_EnumSetOperations() {
		// Test EnumSet operations
		EnumSet<PermissionGroupDatabaseType> allTypes = EnumSet.allOf(PermissionGroupDatabaseType.class);
		assertEquals(3, allTypes.size());
		
		EnumSet<PermissionGroupDatabaseType> userTypes = EnumSet.of(
			PermissionGroupDatabaseType.REGULAR, 
			PermissionGroupDatabaseType.QUERY
		);
		assertEquals(2, userTypes.size());
		assertFalse(userTypes.contains(PermissionGroupDatabaseType.TEMPLATE));
		
		EnumSet<PermissionGroupDatabaseType> templateOnly = EnumSet.of(PermissionGroupDatabaseType.TEMPLATE);
		assertEquals(1, templateOnly.size());
		assertTrue(templateOnly.contains(PermissionGroupDatabaseType.TEMPLATE));
	}
}
