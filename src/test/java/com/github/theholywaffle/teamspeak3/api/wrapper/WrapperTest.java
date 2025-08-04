package com.github.theholywaffle.teamspeak3.api.wrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WrapperTest {

	private Map<String, String> testMap;
	private Wrapper wrapper;

	@BeforeEach
	public void setUp() {
		testMap = new HashMap<>();
		testMap.put("stringProperty", "testValue");
		testMap.put("intProperty", "42");
		testMap.put("longProperty", "1234567890");
		testMap.put("doubleProperty", "3.14");
		testMap.put("booleanTrueProperty", "1");
		testMap.put("booleanFalseProperty", "0");
		testMap.put("emptyProperty", "");
		
		wrapper = new Wrapper(testMap);
	}

	@Test
	public void constructor_ValidMap() {
		assertNotNull(wrapper);
		assertEquals(testMap, wrapper.getMap());
	}

	@Test
	public void getMap_ReturnsUnderlyingMap() {
		assertEquals(testMap, wrapper.getMap());
	}

	@Test
	public void get_ExistingProperty() {
		assertEquals("testValue", wrapper.get("stringProperty"));
	}

	@Test
	public void get_NonExistingProperty() {
		assertEquals("", wrapper.get("nonExistingProperty"));
	}

	@Test
	public void get_EmptyProperty() {
		assertEquals("", wrapper.get("emptyProperty"));
	}

	@Test
	public void getInt_ValidInteger() {
		assertEquals(42, wrapper.getInt("intProperty"));
	}

	@Test
	public void getInt_NonExistingProperty() {
		assertEquals(-1, wrapper.getInt("nonExistingProperty"));
	}

	@Test
	public void getInt_EmptyProperty() {
		assertEquals(-1, wrapper.getInt("emptyProperty"));
	}

	@Test
	public void getInt_InvalidInteger() {
		testMap.put("invalidInt", "notAnInteger");
		assertThrows(NumberFormatException.class, () -> wrapper.getInt("invalidInt"));
	}

	@Test
	public void getLong_ValidLong() {
		assertEquals(1234567890L, wrapper.getLong("longProperty"));
	}

	@Test
	public void getLong_NonExistingProperty() {
		assertEquals(-1L, wrapper.getLong("nonExistingProperty"));
	}

	@Test
	public void getLong_EmptyProperty() {
		assertEquals(-1L, wrapper.getLong("emptyProperty"));
	}

	@Test
	public void getLong_InvalidLong() {
		testMap.put("invalidLong", "notALong");
		assertThrows(NumberFormatException.class, () -> wrapper.getLong("invalidLong"));
	}

	@Test
	public void getDouble_ValidDouble() {
		assertEquals(3.14, wrapper.getDouble("doubleProperty"), 0.001);
	}

	@Test
	public void getDouble_NonExistingProperty() {
		assertEquals(-1.0, wrapper.getDouble("nonExistingProperty"), 0.001);
	}

	@Test
	public void getDouble_EmptyProperty() {
		assertEquals(-1.0, wrapper.getDouble("emptyProperty"), 0.001);
	}

	@Test
	public void getDouble_InvalidDouble() {
		testMap.put("invalidDouble", "notADouble");
		assertThrows(NumberFormatException.class, () -> wrapper.getDouble("invalidDouble"));
	}

	@Test
	public void getBoolean_TrueValue() {
		assertTrue(wrapper.getBoolean("booleanTrueProperty"));
	}

	@Test
	public void getBoolean_FalseValue() {
		assertFalse(wrapper.getBoolean("booleanFalseProperty"));
	}

	@Test
	public void getBoolean_NonExistingProperty() {
		assertFalse(wrapper.getBoolean("nonExistingProperty"));
	}

	@Test
	public void getBoolean_EmptyProperty() {
		assertFalse(wrapper.getBoolean("emptyProperty"));
	}

	@Test
	public void getBoolean_NonNumericValue() {
		testMap.put("nonNumeric", "notANumber");
		assertThrows(NumberFormatException.class, () -> wrapper.getBoolean("nonNumeric"));
	}

	@Test
	public void toString_ReturnsMapToString() {
		assertEquals(testMap.toString(), wrapper.toString());
	}

	@Test
	public void emptyWrapper_ReturnsDefaultValues() {
		assertEquals("", Wrapper.EMPTY.get("anyProperty"));
		assertEquals(-1, Wrapper.EMPTY.getInt("anyProperty"));
		assertEquals(-1L, Wrapper.EMPTY.getLong("anyProperty"));
		assertEquals(-1.0, Wrapper.EMPTY.getDouble("anyProperty"), 0.001);
		assertFalse(Wrapper.EMPTY.getBoolean("anyProperty"));
	}
}
