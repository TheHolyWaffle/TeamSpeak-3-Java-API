package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseClientCommandsTest {

	@Test
	public void clientDBDelete_ValidClientDBId() {
		String expected = "clientdbdelete cldbid=100";
		assertEquals(expected, DatabaseClientCommands.clientDBDelete(100).toString());
	}

	@Test
	public void clientDBDelete_ZeroClientDBId() {
		String expected = "clientdbdelete cldbid=0";
		assertEquals(expected, DatabaseClientCommands.clientDBDelete(0).toString());
	}

	@Test
	public void clientDBDelete_NegativeClientDBId() {
		String expected = "clientdbdelete cldbid=-1";
		assertEquals(expected, DatabaseClientCommands.clientDBDelete(-1).toString());
	}

	@Test
	public void clientDBDelete_LargeClientDBId() {
		String expected = "clientdbdelete cldbid=999999";
		assertEquals(expected, DatabaseClientCommands.clientDBDelete(999999).toString());
	}

	@Test
	public void clientDBDelete_MaxIntClientDBId() {
		String expected = "clientdbdelete cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, DatabaseClientCommands.clientDBDelete(Integer.MAX_VALUE).toString());
	}

	@Test
	public void clientDBEdit_WithOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "NewNickname");
		options.put(ClientProperty.CLIENT_DESCRIPTION, "New Description");
		
		String result = DatabaseClientCommands.clientDBEdit(100, options).toString();
		assertTrue(result.startsWith("clientdbedit cldbid=100"));
		assertTrue(result.contains("client_nickname=NewNickname"));
		assertTrue(result.contains("client_description=New\\sDescription"));
	}

	@Test
	public void clientDBEdit_EmptyOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		String expected = "clientdbedit cldbid=100";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(100, options).toString());
	}

	@Test
	public void clientDBEdit_NullOptions() {
		String expected = "clientdbedit cldbid=100";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(100, null).toString());
	}

	@Test
	public void clientDBEdit_SingleOption() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		
		String expected = "clientdbedit cldbid=100 client_nickname=TestUser";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(100, options).toString());
	}

	@Test
	public void clientDBEdit_SpecialCharactersInOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "User [Admin]");
		options.put(ClientProperty.CLIENT_DESCRIPTION, "VIP User with \"special\" privileges");
		
		String result = DatabaseClientCommands.clientDBEdit(100, options).toString();
		assertTrue(result.contains("client_nickname=User\\s[Admin]"));
		assertTrue(result.contains("client_description=VIP\\sUser\\swith\\s\"special\"\\sprivileges"));
	}

	@Test
	public void clientDBEdit_ZeroClientDBId() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		
		String expected = "clientdbedit cldbid=0 client_nickname=TestUser";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(0, options).toString());
	}

	@Test
	public void clientDBFind_ValidPattern() {
		String expected = "clientdbfind pattern=TestUser";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("TestUser", false).toString());
	}

	@Test
	public void clientDBFind_WithUID() {
		String expected = "clientdbfind pattern=TestUser -uid";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("TestUser", true).toString());
	}

	@Test
	public void clientDBFind_PatternWithSpaces() {
		String expected = "clientdbfind pattern=Test\\sUser";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("Test User", false).toString());
	}

	@Test
	public void clientDBFind_PatternWithWildcards() {
		String expected = "clientdbfind pattern=Test* -uid";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("Test*", true).toString());
	}

	@Test
	public void clientDBFind_PatternWithSpecialCharacters() {
		String expected = "clientdbfind pattern=[Admin]\\sUser";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("[Admin] User", false).toString());
	}

	@Test
	public void clientDBFind_NullPatternException() {
		assertThrows(IllegalArgumentException.class, () -> DatabaseClientCommands.clientDBFind(null, false));
	}

	@Test
	public void clientDBFind_EmptyPatternException() {
		assertThrows(IllegalArgumentException.class, () -> DatabaseClientCommands.clientDBFind("", false));
	}

	@Test
	public void clientDBFind_LongPattern() {
		String longPattern = "This is a very long pattern that contains multiple words and should be properly encoded";
		String expected = "clientdbfind pattern=This\\sis\\sa\\svery\\slong\\spattern\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded";
		assertEquals(expected, DatabaseClientCommands.clientDBFind(longPattern, false).toString());
	}

	@Test
	public void clientDBInfo_ValidClientDBId() {
		String expected = "clientdbinfo cldbid=100";
		assertEquals(expected, DatabaseClientCommands.clientDBInfo(100).toString());
	}

	@Test
	public void clientDBInfo_ZeroClientDBId() {
		String expected = "clientdbinfo cldbid=0";
		assertEquals(expected, DatabaseClientCommands.clientDBInfo(0).toString());
	}

	@Test
	public void clientDBInfo_NegativeClientDBId() {
		String expected = "clientdbinfo cldbid=-1";
		assertEquals(expected, DatabaseClientCommands.clientDBInfo(-1).toString());
	}

	@Test
	public void clientDBInfo_LargeClientDBId() {
		String expected = "clientdbinfo cldbid=999999";
		assertEquals(expected, DatabaseClientCommands.clientDBInfo(999999).toString());
	}

	@Test
	public void clientDBInfo_MaxIntClientDBId() {
		String expected = "clientdbinfo cldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, DatabaseClientCommands.clientDBInfo(Integer.MAX_VALUE).toString());
	}

	@Test
	public void clientDBList_BasicParameters() {
		String expected = "clientdblist start=0 duration=25";
		assertEquals(expected, DatabaseClientCommands.clientDBList(0, 25, false).toString());
	}

	@Test
	public void clientDBList_WithCount() {
		String expected = "clientdblist start=10 duration=50 -count";
		assertEquals(expected, DatabaseClientCommands.clientDBList(10, 50, true).toString());
	}

	@Test
	public void clientDBList_ZeroBegin() {
		String expected = "clientdblist start=0 duration=100";
		assertEquals(expected, DatabaseClientCommands.clientDBList(0, 100, false).toString());
	}

	@Test
	public void clientDBList_NegativeBegin() {
		String expected = "clientdblist start=-1 duration=25";
		assertEquals(expected, DatabaseClientCommands.clientDBList(-1, 25, false).toString());
	}

	@Test
	public void clientDBList_ZeroAmount() {
		String expected = "clientdblist start=10 duration=0";
		assertEquals(expected, DatabaseClientCommands.clientDBList(10, 0, false).toString());
	}

	@Test
	public void clientDBList_NegativeAmount() {
		String expected = "clientdblist start=10 duration=-5";
		assertEquals(expected, DatabaseClientCommands.clientDBList(10, -5, false).toString());
	}

	@Test
	public void clientDBList_LargeValues() {
		String expected = "clientdblist start=999999 duration=888888 -count";
		assertEquals(expected, DatabaseClientCommands.clientDBList(999999, 888888, true).toString());
	}

	@Test
	public void clientDBList_MaxIntValues() {
		String expected = "clientdblist start=" + Integer.MAX_VALUE + " duration=" + (Integer.MAX_VALUE - 1) + " -count";
		assertEquals(expected, DatabaseClientCommands.clientDBList(Integer.MAX_VALUE, Integer.MAX_VALUE - 1, true).toString());
	}

	@Test
	public void clientDBEdit_MultipleOptions() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "AdminUser");
		options.put(ClientProperty.CLIENT_DESCRIPTION, "Server Administrator");
		options.put(ClientProperty.CLIENT_ICON_ID, "100");

		String result = DatabaseClientCommands.clientDBEdit(100, options).toString();
		assertTrue(result.startsWith("clientdbedit cldbid=100"));
		assertTrue(result.contains("client_nickname=AdminUser"));
		assertTrue(result.contains("client_description=Server\\sAdministrator"));
		assertTrue(result.contains("client_icon_id=100"));
	}

	@Test
	public void clientDBFind_PatternWithForwardSlashes() {
		String expected = "clientdbfind pattern=path\\/to\\/user";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("path/to/user", false).toString());
	}

	@Test
	public void clientDBFind_PatternWithPipes() {
		String expected = "clientdbfind pattern=user\\pwith\\ppipes -uid";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("user|with|pipes", true).toString());
	}

	@Test
	public void clientDBFind_PatternWithBackslashes() {
		String expected = "clientdbfind pattern=user\\\\with\\\\backslashes";
		assertEquals(expected, DatabaseClientCommands.clientDBFind("user\\with\\backslashes", false).toString());
	}

	@Test
	public void clientDBEdit_NegativeClientDBId() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		
		String expected = "clientdbedit cldbid=-1 client_nickname=TestUser";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(-1, options).toString());
	}

	@Test
	public void clientDBEdit_MaxIntClientDBId() {
		Map<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, "TestUser");
		
		String expected = "clientdbedit cldbid=" + Integer.MAX_VALUE + " client_nickname=TestUser";
		assertEquals(expected, DatabaseClientCommands.clientDBEdit(Integer.MAX_VALUE, options).toString());
	}
}
