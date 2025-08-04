package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComplaintCommandsTest {

	@Test
	public void complainAdd_ValidParameters() {
		String expected = "complainadd tcldbid=100 message=Inappropriate\\sbehavior";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "Inappropriate behavior").toString());
	}

	@Test
	public void complainAdd_NullMessage() {
		String expected = "complainadd tcldbid=100 message=";
		assertEquals(expected, ComplaintCommands.complainAdd(100, null).toString());
	}

	@Test
	public void complainAdd_EmptyMessage() {
		String expected = "complainadd tcldbid=100 message=";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "").toString());
	}

	@Test
	public void complainAdd_ZeroClientDBId() {
		String expected = "complainadd tcldbid=0 message=Test\\smessage";
		assertEquals(expected, ComplaintCommands.complainAdd(0, "Test message").toString());
	}

	@Test
	public void complainAdd_NegativeClientDBId() {
		String expected = "complainadd tcldbid=-1 message=Test\\smessage";
		assertEquals(expected, ComplaintCommands.complainAdd(-1, "Test message").toString());
	}

	@Test
	public void complainAdd_SpecialCharactersInMessage() {
		String expected = "complainadd tcldbid=100 message=User\\swas\\s\"very\"\\sbad\\s&\\sspamming";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "User was \"very\" bad & spamming").toString());
	}

	@Test
	public void complainAdd_LongMessage() {
		String longMessage = "This user has been consistently violating server rules by spamming channels, using inappropriate language, and disrupting other users' conversations.";
		String expected = "complainadd tcldbid=100 message=This\\suser\\shas\\sbeen\\sconsistently\\sviolating\\sserver\\srules\\sby\\sspamming\\schannels,\\susing\\sinappropriate\\slanguage,\\sand\\sdisrupting\\sother\\susers'\\sconversations.";
		assertEquals(expected, ComplaintCommands.complainAdd(100, longMessage).toString());
	}

	@Test
	public void complainAdd_MessageWithNewlines() {
		String messageWithNewlines = "Line 1\nLine 2\nLine 3";
		String expected = "complainadd tcldbid=100 message=Line\\s1\\nLine\\s2\\nLine\\s3";
		assertEquals(expected, ComplaintCommands.complainAdd(100, messageWithNewlines).toString());
	}

	@Test
	public void complainAdd_MessageWithTabs() {
		String messageWithTabs = "Column1\tColumn2\tColumn3";
		String expected = "complainadd tcldbid=100 message=Column1\\tColumn2\\tColumn3";
		assertEquals(expected, ComplaintCommands.complainAdd(100, messageWithTabs).toString());
	}

	@Test
	public void complainDel_ValidParameters() {
		String expected = "complaindel tcldbid=100 fcldbid=200";
		assertEquals(expected, ComplaintCommands.complainDel(100, 200).toString());
	}

	@Test
	public void complainDel_ZeroClientDBIds() {
		String expected = "complaindel tcldbid=0 fcldbid=0";
		assertEquals(expected, ComplaintCommands.complainDel(0, 0).toString());
	}

	@Test
	public void complainDel_NegativeClientDBIds() {
		String expected = "complaindel tcldbid=-1 fcldbid=-2";
		assertEquals(expected, ComplaintCommands.complainDel(-1, -2).toString());
	}

	@Test
	public void complainDel_LargeClientDBIds() {
		String expected = "complaindel tcldbid=999999 fcldbid=888888";
		assertEquals(expected, ComplaintCommands.complainDel(999999, 888888).toString());
	}

	@Test
	public void complainDel_SameClientDBIds() {
		String expected = "complaindel tcldbid=100 fcldbid=100";
		assertEquals(expected, ComplaintCommands.complainDel(100, 100).toString());
	}

	@Test
	public void complainDelAll_ValidClientDBId() {
		String expected = "complaindelall tcldbid=100";
		assertEquals(expected, ComplaintCommands.complainDelAll(100).toString());
	}

	@Test
	public void complainDelAll_ZeroClientDBId() {
		String expected = "complaindelall tcldbid=0";
		assertEquals(expected, ComplaintCommands.complainDelAll(0).toString());
	}

	@Test
	public void complainDelAll_NegativeClientDBId() {
		String expected = "complaindelall tcldbid=-1";
		assertEquals(expected, ComplaintCommands.complainDelAll(-1).toString());
	}

	@Test
	public void complainDelAll_LargeClientDBId() {
		String expected = "complaindelall tcldbid=999999";
		assertEquals(expected, ComplaintCommands.complainDelAll(999999).toString());
	}

	@Test
	public void complainList_ValidClientDBId() {
		String expected = "complainlist tcldbid=100";
		assertEquals(expected, ComplaintCommands.complainList(100).toString());
	}

	@Test
	public void complainList_ZeroClientDBId() {
		// Zero client DB ID should not be included
		String expected = "complainlist";
		assertEquals(expected, ComplaintCommands.complainList(0).toString());
	}

	@Test
	public void complainList_NegativeClientDBId() {
		// Negative client DB ID should not be included
		String expected = "complainlist";
		assertEquals(expected, ComplaintCommands.complainList(-1).toString());
	}

	@Test
	public void complainList_LargeClientDBId() {
		String expected = "complainlist tcldbid=999999";
		assertEquals(expected, ComplaintCommands.complainList(999999).toString());
	}

	@Test
	public void complainAdd_MessageWithForwardSlashes() {
		String expected = "complainadd tcldbid=100 message=User\\sspamming\\s\\/channel\\swith\\slinks";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "User spamming /channel with links").toString());
	}

	@Test
	public void complainAdd_MessageWithPipes() {
		String expected = "complainadd tcldbid=100 message=User\\susing\\s\\p\\scharacter\\sin\\sname";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "User using | character in name").toString());
	}

	@Test
	public void complainAdd_MessageWithBackslashes() {
		String expected = "complainadd tcldbid=100 message=User\\susing\\s\\\\\\scharacter";
		assertEquals(expected, ComplaintCommands.complainAdd(100, "User using \\ character").toString());
	}

	@Test
	public void complainAdd_MaxIntClientDBId() {
		String expected = "complainadd tcldbid=" + Integer.MAX_VALUE + " message=Test";
		assertEquals(expected, ComplaintCommands.complainAdd(Integer.MAX_VALUE, "Test").toString());
	}

	@Test
	public void complainDel_MaxIntClientDBIds() {
		String expected = "complaindel tcldbid=" + Integer.MAX_VALUE + " fcldbid=" + (Integer.MAX_VALUE - 1);
		assertEquals(expected, ComplaintCommands.complainDel(Integer.MAX_VALUE, Integer.MAX_VALUE - 1).toString());
	}

	@Test
	public void complainDelAll_MaxIntClientDBId() {
		String expected = "complaindelall tcldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, ComplaintCommands.complainDelAll(Integer.MAX_VALUE).toString());
	}

	@Test
	public void complainList_MaxIntClientDBId() {
		String expected = "complainlist tcldbid=" + Integer.MAX_VALUE;
		assertEquals(expected, ComplaintCommands.complainList(Integer.MAX_VALUE).toString());
	}
}
