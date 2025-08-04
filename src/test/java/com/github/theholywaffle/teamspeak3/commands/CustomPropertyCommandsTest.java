package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CustomPropertyCommandsTest {

	@Test
	public void customDelete_ValidParameters() {
		String expected = "customdelete cldbid=100 ident=mykey";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, "mykey").toString());
	}

	@Test
	public void customDelete_NullKey() {
		String expected = "customdelete cldbid=100 ident=";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, null).toString());
	}

	@Test
	public void customDelete_EmptyKey() {
		String expected = "customdelete cldbid=100 ident=";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, "").toString());
	}

	@Test
	public void customDelete_ZeroClientDBId() {
		String expected = "customdelete cldbid=0 ident=mykey";
		assertEquals(expected, CustomPropertyCommands.customDelete(0, "mykey").toString());
	}

	@Test
	public void customDelete_NegativeClientDBId() {
		String expected = "customdelete cldbid=-1 ident=mykey";
		assertEquals(expected, CustomPropertyCommands.customDelete(-1, "mykey").toString());
	}

	@Test
	public void customDelete_SpecialCharactersInKey() {
		String expected = "customdelete cldbid=100 ident=my\\skey\\s[special]";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, "my key [special]").toString());
	}

	@Test
	public void customDelete_KeyWithForwardSlashes() {
		String expected = "customdelete cldbid=100 ident=path\\/to\\/key";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, "path/to/key").toString());
	}

	@Test
	public void customDelete_KeyWithPipes() {
		String expected = "customdelete cldbid=100 ident=key\\pwith\\ppipes";
		assertEquals(expected, CustomPropertyCommands.customDelete(100, "key|with|pipes").toString());
	}

	@Test
	public void customInfo_ValidClientDBId() {
		String expected = "custominfo cldbid=100";
		assertEquals(expected, CustomPropertyCommands.customInfo(100).toString());
	}

	@Test
	public void customInfo_ZeroClientDBId() {
		String expected = "custominfo cldbid=0";
		assertEquals(expected, CustomPropertyCommands.customInfo(0).toString());
	}

	@Test
	public void customInfo_NegativeClientDBId() {
		String expected = "custominfo cldbid=-1";
		assertEquals(expected, CustomPropertyCommands.customInfo(-1).toString());
	}

	@Test
	public void customInfo_LargeClientDBId() {
		String expected = "custominfo cldbid=999999";
		assertEquals(expected, CustomPropertyCommands.customInfo(999999).toString());
	}

	@Test
	public void customInfo_MaxIntClientDBId() {
		String expected = "custominfo cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, CustomPropertyCommands.customInfo(Integer.MAX_VALUE).toString());
	}

	@Test
	public void customSet_ValidParameters() {
		String expected = "customset cldbid=100 ident=mykey value=myvalue";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", "myvalue").toString());
	}

	@Test
	public void customSet_NullKey() {
		String expected = "customset cldbid=100 ident= value=myvalue";
		assertEquals(expected, CustomPropertyCommands.customSet(100, null, "myvalue").toString());
	}

	@Test
	public void customSet_EmptyKey() {
		String expected = "customset cldbid=100 ident= value=myvalue";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "", "myvalue").toString());
	}

	@Test
	public void customSet_NullValue() {
		String expected = "customset cldbid=100 ident=mykey value=";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", null).toString());
	}

	@Test
	public void customSet_EmptyValue() {
		String expected = "customset cldbid=100 ident=mykey value=";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", "").toString());
	}

	@Test
	public void customSet_SpecialCharactersInKey() {
		String expected = "customset cldbid=100 ident=my\\skey\\s[config] value=test";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "my key [config]", "test").toString());
	}

	@Test
	public void customSet_SpecialCharactersInValue() {
		String expected = "customset cldbid=100 ident=mykey value=my\\svalue\\swith\\s\"quotes\"\\s&\\sspecial";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", "my value with \"quotes\" & special").toString());
	}

	@Test
	public void customSet_ValueWithNewlines() {
		String expected = "customset cldbid=100 ident=mykey value=line1\\nline2\\nline3";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", "line1\nline2\nline3").toString());
	}

	@Test
	public void customSet_ValueWithTabs() {
		String expected = "customset cldbid=100 ident=mykey value=col1\\tcol2\\tcol3";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "mykey", "col1\tcol2\tcol3").toString());
	}

	@Test
	public void customSet_LongValue() {
		String longValue = "This is a very long custom property value that contains multiple words and should be properly encoded when sent to the server.";
		String expected = "customset cldbid=100 ident=description value=This\\sis\\sa\\svery\\slong\\scustom\\sproperty\\svalue\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded\\swhen\\ssent\\sto\\sthe\\sserver.";
		assertEquals(expected, CustomPropertyCommands.customSet(100, "description", longValue).toString());
	}

	@Test
	public void customSearch_ValidParameters() {
		String expected = "customsearch ident=mykey pattern=*value*";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", "*value*").toString());
	}

	@Test
	public void customSearch_NullKey() {
		String expected = "customsearch ident= pattern=*value*";
		assertEquals(expected, CustomPropertyCommands.customSearch(null, "*value*").toString());
	}

	@Test
	public void customSearch_EmptyKey() {
		String expected = "customsearch ident= pattern=*value*";
		assertEquals(expected, CustomPropertyCommands.customSearch("", "*value*").toString());
	}

	@Test
	public void customSearch_NullPattern() {
		String expected = "customsearch ident=mykey pattern=";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", null).toString());
	}

	@Test
	public void customSearch_EmptyPattern() {
		String expected = "customsearch ident=mykey pattern=";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", "").toString());
	}

	@Test
	public void customSearch_SpecialCharactersInKey() {
		String expected = "customsearch ident=my\\skey\\s[search] pattern=test";
		assertEquals(expected, CustomPropertyCommands.customSearch("my key [search]", "test").toString());
	}

	@Test
	public void customSearch_SpecialCharactersInPattern() {
		String expected = "customsearch ident=mykey pattern=*test\\svalue*";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", "*test value*").toString());
	}

	@Test
	public void customSearch_PatternWithWildcards() {
		String expected = "customsearch ident=mykey pattern=prefix*suffix";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", "prefix*suffix").toString());
	}

	@Test
	public void customSearch_PatternWithQuestionMark() {
		String expected = "customsearch ident=mykey pattern=test?value";
		assertEquals(expected, CustomPropertyCommands.customSearch("mykey", "test?value").toString());
	}

	@Test
	public void customSearch_ComplexPattern() {
		String expected = "customsearch ident=config pattern=*[admin]*";
		assertEquals(expected, CustomPropertyCommands.customSearch("config", "*[admin]*").toString());
	}

	@Test
	public void customDelete_LargeClientDBId() {
		String expected = "customdelete cldbid=999999 ident=mykey";
		assertEquals(expected, CustomPropertyCommands.customDelete(999999, "mykey").toString());
	}

	@Test
	public void customSet_ZeroClientDBId() {
		String expected = "customset cldbid=0 ident=mykey value=myvalue";
		assertEquals(expected, CustomPropertyCommands.customSet(0, "mykey", "myvalue").toString());
	}

	@Test
	public void customSet_MaxIntClientDBId() {
		String expected = "customset cldbid=" + Integer.MAX_VALUE + " ident=mykey value=myvalue";
		assertEquals(expected, CustomPropertyCommands.customSet(Integer.MAX_VALUE, "mykey", "myvalue").toString());
	}

	@Test
	public void customDelete_MaxIntClientDBId() {
		String expected = "customdelete cldbid=" + Integer.MAX_VALUE + " ident=mykey";
		assertEquals(expected, CustomPropertyCommands.customDelete(Integer.MAX_VALUE, "mykey").toString());
	}
}
