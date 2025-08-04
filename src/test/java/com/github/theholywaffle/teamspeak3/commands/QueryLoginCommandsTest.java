package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueryLoginCommandsTest {

	@Test
	public void queryLoginAdd_ValidParameters() {
		String expected = "queryloginadd client_login_name=admin cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin", 100).toString());
	}

	@Test
	public void queryLoginAdd_ZeroClientDBId() {
		// Zero client DB ID should not be included
		String expected = "queryloginadd client_login_name=admin";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin", 0).toString());
	}

	@Test
	public void queryLoginAdd_NegativeClientDBId() {
		// Negative client DB ID should not be included
		String expected = "queryloginadd client_login_name=admin";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin", -1).toString());
	}

	@Test
	public void queryLoginAdd_NullLoginName() {
		String expected = "queryloginadd client_login_name= cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd(null, 100).toString());
	}

	@Test
	public void queryLoginAdd_EmptyLoginName() {
		String expected = "queryloginadd client_login_name= cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithSpaces() {
		String expected = "queryloginadd client_login_name=admin\\suser cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin user", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithSpecialCharacters() {
		String expected = "queryloginadd client_login_name=admin[user] cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin[user]", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithForwardSlashes() {
		String expected = "queryloginadd client_login_name=admin\\/user cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin/user", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithPipes() {
		String expected = "queryloginadd client_login_name=admin\\puser cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin|user", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithBackslashes() {
		String expected = "queryloginadd client_login_name=admin\\\\user cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin\\user", 100).toString());
	}

	@Test
	public void queryLoginAdd_LongLoginName() {
		String longName = "this-is-a-very-long-login-name-that-contains-many-characters";
		String expected = "queryloginadd client_login_name=" + longName + " cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd(longName, 100).toString());
	}

	@Test
	public void queryLoginAdd_LargeClientDBId() {
		String expected = "queryloginadd client_login_name=admin cldbid=999999";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin", 999999).toString());
	}

	@Test
	public void queryLoginAdd_MaxIntClientDBId() {
		String expected = "queryloginadd client_login_name=admin cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin", Integer.MAX_VALUE).toString());
	}

	@Test
	public void queryLoginDel_ValidClientDBId() {
		String expected = "querylogindel cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginDel(100).toString());
	}

	@Test
	public void queryLoginDel_ZeroClientDBId() {
		String expected = "querylogindel cldbid=0";
		assertEquals(expected, QueryLoginCommands.queryLoginDel(0).toString());
	}

	@Test
	public void queryLoginDel_NegativeClientDBId() {
		String expected = "querylogindel cldbid=-1";
		assertEquals(expected, QueryLoginCommands.queryLoginDel(-1).toString());
	}

	@Test
	public void queryLoginDel_LargeClientDBId() {
		String expected = "querylogindel cldbid=999999";
		assertEquals(expected, QueryLoginCommands.queryLoginDel(999999).toString());
	}

	@Test
	public void queryLoginDel_MaxIntClientDBId() {
		String expected = "querylogindel cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, QueryLoginCommands.queryLoginDel(Integer.MAX_VALUE).toString());
	}

	@Test
	public void queryLoginList_NoPattern() {
		String expected = "queryloginlist";
		assertEquals(expected, QueryLoginCommands.queryLoginList(null).toString());
	}

	@Test
	public void queryLoginList_EmptyPattern() {
		String expected = "queryloginlist pattern=";
		assertEquals(expected, QueryLoginCommands.queryLoginList("").toString());
	}

	@Test
	public void queryLoginList_ValidPattern() {
		String expected = "queryloginlist pattern=admin*";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin*").toString());
	}

	@Test
	public void queryLoginList_PatternWithSpaces() {
		String expected = "queryloginlist pattern=admin\\suser";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin user").toString());
	}

	@Test
	public void queryLoginList_PatternWithWildcards() {
		String expected = "queryloginlist pattern=*admin*";
		assertEquals(expected, QueryLoginCommands.queryLoginList("*admin*").toString());
	}

	@Test
	public void queryLoginList_PatternWithQuestionMark() {
		String expected = "queryloginlist pattern=admin?";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin?").toString());
	}

	@Test
	public void queryLoginList_PatternWithSpecialCharacters() {
		String expected = "queryloginlist pattern=admin[user]";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin[user]").toString());
	}

	@Test
	public void queryLoginList_PatternWithForwardSlashes() {
		String expected = "queryloginlist pattern=admin\\/user";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin/user").toString());
	}

	@Test
	public void queryLoginList_PatternWithPipes() {
		String expected = "queryloginlist pattern=admin\\puser";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin|user").toString());
	}

	@Test
	public void queryLoginList_PatternWithBackslashes() {
		String expected = "queryloginlist pattern=admin\\\\user";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin\\user").toString());
	}

	@Test
	public void queryLoginList_LongPattern() {
		String longPattern = "this-is-a-very-long-pattern-that-contains-many-characters-and-wildcards*";
		String expected = "queryloginlist pattern=" + longPattern;
		assertEquals(expected, QueryLoginCommands.queryLoginList(longPattern).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithQuotes() {
		String expected = "queryloginadd client_login_name=admin\"user\" cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin\"user\"", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithAmpersands() {
		String expected = "queryloginadd client_login_name=admin&user cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin&user", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithNewlines() {
		String expected = "queryloginadd client_login_name=admin\\nuser cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin\nuser", 100).toString());
	}

	@Test
	public void queryLoginAdd_LoginNameWithTabs() {
		String expected = "queryloginadd client_login_name=admin\\tuser cldbid=100";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("admin\tuser", 100).toString());
	}

	@Test
	public void queryLoginList_PatternWithQuotes() {
		String expected = "queryloginlist pattern=admin\"user\"";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin\"user\"").toString());
	}

	@Test
	public void queryLoginList_PatternWithAmpersands() {
		String expected = "queryloginlist pattern=admin&user";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin&user").toString());
	}

	@Test
	public void queryLoginList_PatternWithNewlines() {
		String expected = "queryloginlist pattern=admin\\nuser";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin\nuser").toString());
	}

	@Test
	public void queryLoginList_PatternWithTabs() {
		String expected = "queryloginlist pattern=admin\\tuser";
		assertEquals(expected, QueryLoginCommands.queryLoginList("admin\tuser").toString());
	}

	@Test
	public void queryLoginAdd_BothNullAndZero() {
		String expected = "queryloginadd client_login_name=";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd(null, 0).toString());
	}

	@Test
	public void queryLoginAdd_EmptyNameAndNegativeId() {
		String expected = "queryloginadd client_login_name=";
		assertEquals(expected, QueryLoginCommands.queryLoginAdd("", -1).toString());
	}
}
