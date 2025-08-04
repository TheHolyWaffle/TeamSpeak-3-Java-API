package com.github.theholywaffle.teamspeak3.commands.parameter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParameterTest {

	// KeyValueParam Tests
	@Test
	public void keyValueParam_StringValue() {
		KeyValueParam param = new KeyValueParam("key", "value");
		assertEquals("key=value", param.toString());
	}

	@Test
	public void keyValueParam_NullValue() {
		KeyValueParam param = new KeyValueParam("key", (String) null);
		assertEquals("key=", param.toString());
	}

	@Test
	public void keyValueParam_EmptyValue() {
		KeyValueParam param = new KeyValueParam("key", "");
		assertEquals("key=", param.toString());
	}

	@Test
	public void keyValueParam_IntValue() {
		KeyValueParam param = new KeyValueParam("count", 42);
		assertEquals("count=42", param.toString());
	}

	@Test
	public void keyValueParam_NegativeIntValue() {
		KeyValueParam param = new KeyValueParam("offset", -10);
		assertEquals("offset=-10", param.toString());
	}

	@Test
	public void keyValueParam_ZeroIntValue() {
		KeyValueParam param = new KeyValueParam("zero", 0);
		assertEquals("zero=0", param.toString());
	}

	@Test
	public void keyValueParam_LongValue() {
		KeyValueParam param = new KeyValueParam("timestamp", 1234567890123L);
		assertEquals("timestamp=1234567890123", param.toString());
	}

	@Test
	public void keyValueParam_MaxLongValue() {
		KeyValueParam param = new KeyValueParam("max", Long.MAX_VALUE);
		assertEquals("max=" + Long.MAX_VALUE, param.toString());
	}

	@Test
	public void keyValueParam_BooleanTrueValue() {
		KeyValueParam param = new KeyValueParam("enabled", true);
		assertEquals("enabled=1", param.toString());
	}

	@Test
	public void keyValueParam_BooleanFalseValue() {
		KeyValueParam param = new KeyValueParam("disabled", false);
		assertEquals("disabled=0", param.toString());
	}

	@Test
	public void keyValueParam_NullKeyException() {
		assertThrows(IllegalArgumentException.class, () -> new KeyValueParam(null, "value"));
	}

	@Test
	public void keyValueParam_SpecialCharactersInKey() {
		KeyValueParam param = new KeyValueParam("my key", "value");
		assertEquals("my\\skey=value", param.toString());
	}

	@Test
	public void keyValueParam_SpecialCharactersInValue() {
		KeyValueParam param = new KeyValueParam("key", "value with spaces");
		assertEquals("key=value\\swith\\sspaces", param.toString());
	}

	@Test
	public void keyValueParam_SpecialCharactersInBoth() {
		KeyValueParam param = new KeyValueParam("my/key", "value|with|pipes");
		assertEquals("my\\/key=value\\pwith\\ppipes", param.toString());
	}

	// OptionParam Tests
	@Test
	public void optionParam_SimpleOption() {
		OptionParam param = new OptionParam("help");
		assertEquals("-help", param.toString());
	}

	@Test
	public void optionParam_NullOption() {
		// OptionParam doesn't handle null - it throws NPE
		assertThrows(NullPointerException.class, () -> new OptionParam(null).toString());
	}

	@Test
	public void optionParam_EmptyOption() {
		OptionParam param = new OptionParam("");
		assertEquals("-", param.toString());
	}

	@Test
	public void optionParam_OptionWithSpaces() {
		OptionParam param = new OptionParam("long option");
		assertEquals("-long\\soption", param.toString());
	}

	@Test
	public void optionParam_OptionWithSpecialCharacters() {
		OptionParam param = new OptionParam("option/with/slashes");
		assertEquals("-option\\/with\\/slashes", param.toString());
	}

	// ValueParam Tests
	@Test
	public void valueParam_SimpleValue() {
		ValueParam param = new ValueParam("rawvalue");
		assertEquals("rawvalue", param.toString());
	}

	@Test
	public void valueParam_NullValue() {
		// ValueParam doesn't handle null - it throws NPE
		assertThrows(NullPointerException.class, () -> new ValueParam(null).toString());
	}

	@Test
	public void valueParam_EmptyValue() {
		ValueParam param = new ValueParam("");
		assertEquals("", param.toString());
	}

	@Test
	public void valueParam_ValueWithSpaces() {
		ValueParam param = new ValueParam("value with spaces");
		assertEquals("value\\swith\\sspaces", param.toString());
	}

	@Test
	public void valueParam_ValueWithSpecialCharacters() {
		ValueParam param = new ValueParam("value|with\\special/chars");
		assertEquals("value\\pwith\\\\special\\/chars", param.toString());
	}

	// RawParam Tests
	@Test
	public void rawParam_SimpleValue() {
		RawParam param = new RawParam("rawdata");
		assertEquals("rawdata", param.toString());
	}

	@Test
	public void rawParam_NullValue() {
		RawParam param = new RawParam(null);
		assertEquals("null", param.toString());
	}

	@Test
	public void rawParam_EmptyValue() {
		RawParam param = new RawParam("");
		assertEquals("", param.toString());
	}

	@Test
	public void rawParam_ValueWithSpaces() {
		// RawParam does NOT encode - it outputs raw
		RawParam param = new RawParam("value with spaces");
		assertEquals("value with spaces", param.toString());
	}

	@Test
	public void rawParam_ValueWithSpecialCharacters() {
		// RawParam does NOT encode - it outputs raw
		RawParam param = new RawParam("value|with\\special/chars");
		assertEquals("value|with\\special/chars", param.toString());
	}

	// ArrayParameter Tests
	@Test
	public void arrayParameter_EmptyArray() {
		ArrayParameter param = new ArrayParameter(0);
		assertEquals("", param.toString());
	}

	@Test
	public void arrayParameter_SingleEntry() {
		ArrayParameter param = new ArrayParameter(1);
		param.add(new KeyValueParam("key", "value"));
		assertEquals("key=value", param.toString());
	}

	@Test
	public void arrayParameter_MultipleEntries() {
		ArrayParameter param = new ArrayParameter(3);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new KeyValueParam("key3", "value3"));
		assertEquals("key1=value1|key2=value2|key3=value3", param.toString());
	}

	@Test
	public void arrayParameter_MultipleParametersPerEntry() {
		ArrayParameter param = new ArrayParameter(2, 2);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new KeyValueParam("key3", "value3"));
		param.add(new KeyValueParam("key4", "value4"));
		assertEquals("key1=value1 key2=value2|key3=value3 key4=value4", param.toString());
	}

	@Test
	public void arrayParameter_MixedParameterTypes() {
		ArrayParameter param = new ArrayParameter(2, 3);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new OptionParam("option1"));
		param.add(new ValueParam("raw1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new OptionParam("option2"));
		param.add(new ValueParam("raw2"));
		assertEquals("key1=value1 -option1 raw1|key2=value2 -option2 raw2", param.toString());
	}

	@Test
	public void arrayParameter_SingleParameterPerEntry() {
		ArrayParameter param = new ArrayParameter(3, 1);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new KeyValueParam("key3", "value3"));
		assertEquals("key1=value1|key2=value2|key3=value3", param.toString());
	}

	@Test
	public void arrayParameter_ChainedAdd() {
		ArrayParameter param = new ArrayParameter(2);
		param.add(new KeyValueParam("key1", "value1"))
			 .add(new KeyValueParam("key2", "value2"));
		assertEquals("key1=value1|key2=value2", param.toString());
	}

	@Test
	public void arrayParameter_LargeArray() {
		ArrayParameter param = new ArrayParameter(5);
		for (int i = 1; i <= 5; i++) {
			param.add(new KeyValueParam("key" + i, "value" + i));
		}
		assertEquals("key1=value1|key2=value2|key3=value3|key4=value4|key5=value5", param.toString());
	}

	// Integration Tests
	@Test
	public void parameter_AppendToStringBuilder() {
		StringBuilder sb = new StringBuilder("prefix ");
		KeyValueParam param = new KeyValueParam("key", "value");
		param.appendTo(sb);
		sb.append(" suffix");
		assertEquals("prefix key=value suffix", sb.toString());
	}

	@Test
	public void parameter_ToStringConsistency() {
		KeyValueParam param = new KeyValueParam("key", "value");
		String result1 = param.toString();
		String result2 = param.toString();
		assertEquals(result1, result2);
	}

	@Test
	public void arrayParameter_IncompleteEntries() {
		// Test with fewer parameters than expected - this causes IndexOutOfBoundsException
		ArrayParameter param = new ArrayParameter(2, 2);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new KeyValueParam("key3", "value3")); // Only 1 param for second entry
		assertThrows(IndexOutOfBoundsException.class, () -> param.toString());
	}

	@Test
	public void arrayParameter_ExtraParameters() {
		// Test with more parameters than expected entries
		ArrayParameter param = new ArrayParameter(1, 2);
		param.add(new KeyValueParam("key1", "value1"));
		param.add(new KeyValueParam("key2", "value2"));
		param.add(new KeyValueParam("key3", "value3"));
		param.add(new KeyValueParam("key4", "value4"));
		assertEquals("key1=value1 key2=value2|key3=value3 key4=value4", param.toString());
	}
}
