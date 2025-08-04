package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivilegeKeyCommandsTest {

	@Test
	public void privilegeKeyAdd_ServerGroupType() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, null).toString());
	}

	@Test
	public void privilegeKeyAdd_ChannelGroupType() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=10 tokenid2=15";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, 10, 15, null).toString());
	}

	@Test
	public void privilegeKeyAdd_WithDescription() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0 tokendescription=Admin\\sKey";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, "Admin Key").toString());
	}

	@Test
	public void privilegeKeyAdd_EmptyDescription() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0 tokendescription=";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, "").toString());
	}

	@Test
	public void privilegeKeyAdd_NullDescription() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, null).toString());
	}

	@Test
	public void privilegeKeyAdd_NullTypeException() {
		assertThrows(IllegalArgumentException.class, () -> PrivilegeKeyCommands.privilegeKeyAdd(null, 5, 0, "Test"));
	}

	@Test
	public void privilegeKeyAdd_SpecialCharactersInDescription() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=10 tokenid2=15 tokendescription=VIP\\sKey\\s[Special]";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, 10, 15, "VIP Key [Special]").toString());
	}

	@Test
	public void privilegeKeyAdd_LongDescription() {
		String longDescription = "This is a very long privilege key description that contains multiple words and should be properly encoded";
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0 tokendescription=This\\sis\\sa\\svery\\slong\\sprivilege\\skey\\sdescription\\sthat\\scontains\\smultiple\\swords\\sand\\sshould\\sbe\\sproperly\\sencoded";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, longDescription).toString());
	}

	@Test
	public void privilegeKeyAdd_ZeroGroupIds() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=0 tokenid2=0";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 0, 0, null).toString());
	}

	@Test
	public void privilegeKeyAdd_NegativeGroupIds() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=-1 tokenid2=-2";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, -1, -2, null).toString());
	}

	@Test
	public void privilegeKeyAdd_LargeGroupIds() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=999999 tokenid2=888888";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 999999, 888888, null).toString());
	}

	@Test
	public void privilegeKeyAdd_MaxIntGroupIds() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=" + Integer.MAX_VALUE + " tokenid2=" + (Integer.MAX_VALUE - 1);
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, null).toString());
	}

	@Test
	public void privilegeKeyDelete_ValidToken() {
		String expected = "privilegekeydelete token=abc123def456";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("abc123def456").toString());
	}

	@Test
	public void privilegeKeyDelete_TokenWithSpecialCharacters() {
		String expected = "privilegekeydelete token=token\\swith\\sspaces";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("token with spaces").toString());
	}

	@Test
	public void privilegeKeyDelete_TokenWithForwardSlashes() {
		String expected = "privilegekeydelete token=token\\/with\\/slashes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("token/with/slashes").toString());
	}

	@Test
	public void privilegeKeyDelete_TokenWithPipes() {
		String expected = "privilegekeydelete token=token\\pwith\\ppipes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("token|with|pipes").toString());
	}

	@Test
	public void privilegeKeyDelete_TokenWithBackslashes() {
		String expected = "privilegekeydelete token=token\\\\with\\\\backslashes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("token\\with\\backslashes").toString());
	}

	@Test
	public void privilegeKeyDelete_NullTokenException() {
		assertThrows(IllegalArgumentException.class, () -> PrivilegeKeyCommands.privilegeKeyDelete(null));
	}

	@Test
	public void privilegeKeyDelete_EmptyTokenException() {
		assertThrows(IllegalArgumentException.class, () -> PrivilegeKeyCommands.privilegeKeyDelete(""));
	}

	@Test
	public void privilegeKeyDelete_LongToken() {
		String longToken = "this-is-a-very-long-privilege-key-token-that-contains-many-characters-and-should-be-properly-encoded";
		String expected = "privilegekeydelete token=" + longToken;
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete(longToken).toString());
	}

	@Test
	public void privilegeKeyList() {
		String expected = "privilegekeylist";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyList().toString());
	}

	@Test
	public void privilegeKeyUse_ValidToken() {
		String expected = "privilegekeyuse token=xyz789abc123";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("xyz789abc123").toString());
	}

	@Test
	public void privilegeKeyUse_TokenWithSpecialCharacters() {
		String expected = "privilegekeyuse token=token\\swith\\sspaces";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("token with spaces").toString());
	}

	@Test
	public void privilegeKeyUse_TokenWithForwardSlashes() {
		String expected = "privilegekeyuse token=token\\/with\\/slashes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("token/with/slashes").toString());
	}

	@Test
	public void privilegeKeyUse_TokenWithPipes() {
		String expected = "privilegekeyuse token=token\\pwith\\ppipes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("token|with|pipes").toString());
	}

	@Test
	public void privilegeKeyUse_TokenWithBackslashes() {
		String expected = "privilegekeyuse token=token\\\\with\\\\backslashes";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("token\\with\\backslashes").toString());
	}

	@Test
	public void privilegeKeyUse_NullTokenException() {
		assertThrows(IllegalArgumentException.class, () -> PrivilegeKeyCommands.privilegeKeyUse(null));
	}

	@Test
	public void privilegeKeyUse_EmptyTokenException() {
		assertThrows(IllegalArgumentException.class, () -> PrivilegeKeyCommands.privilegeKeyUse(""));
	}

	@Test
	public void privilegeKeyUse_LongToken() {
		String longToken = "this-is-a-very-long-privilege-key-token-that-contains-many-characters-and-should-be-properly-encoded";
		String expected = "privilegekeyuse token=" + longToken;
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse(longToken).toString());
	}

	@Test
	public void privilegeKeyAdd_DescriptionWithQuotes() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0 tokendescription=Key\\swith\\s\"quotes\"";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, "Key with \"quotes\"").toString());
	}

	@Test
	public void privilegeKeyAdd_DescriptionWithAmpersands() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=10 tokenid2=15 tokendescription=Key\\s&\\sDescription";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, 10, 15, "Key & Description").toString());
	}

	@Test
	public void privilegeKeyAdd_DescriptionWithNewlines() {
		String expected = "privilegekeyadd tokentype=0 tokenid1=5 tokenid2=0 tokendescription=Line1\\nLine2\\nLine3";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.SERVER_GROUP, 5, 0, "Line1\nLine2\nLine3").toString());
	}

	@Test
	public void privilegeKeyAdd_DescriptionWithTabs() {
		String expected = "privilegekeyadd tokentype=1 tokenid1=10 tokenid2=15 tokendescription=Col1\\tCol2\\tCol3";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyType.CHANNEL_GROUP, 10, 15, "Col1\tCol2\tCol3").toString());
	}

	@Test
	public void privilegeKeyDelete_TokenWithQuotes() {
		String expected = "privilegekeydelete token=token\\swith\\s\"quotes\"";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyDelete("token with \"quotes\"").toString());
	}

	@Test
	public void privilegeKeyUse_TokenWithQuotes() {
		String expected = "privilegekeyuse token=token\\swith\\s\"quotes\"";
		assertEquals(expected, PrivilegeKeyCommands.privilegeKeyUse("token with \"quotes\"").toString());
	}
}
