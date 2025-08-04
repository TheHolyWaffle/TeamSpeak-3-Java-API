package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageCommandsTest {

	@Test
	public void messageAdd_ValidParameters() {
		String expected = "messageadd cluid=unique123 subject=Test\\sSubject message=Test\\smessage\\scontent";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Test Subject", "Test message content").toString());
	}

	@Test
	public void messageAdd_NullSubject() {
		String expected = "messageadd cluid=unique123 subject= message=Test\\smessage";
		assertEquals(expected, MessageCommands.messageAdd("unique123", null, "Test message").toString());
	}

	@Test
	public void messageAdd_EmptySubject() {
		String expected = "messageadd cluid=unique123 subject= message=Test\\smessage";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "", "Test message").toString());
	}

	@Test
	public void messageAdd_NullMessage() {
		String expected = "messageadd cluid=unique123 subject=Test\\sSubject message=";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Test Subject", null).toString());
	}

	@Test
	public void messageAdd_EmptyMessage() {
		String expected = "messageadd cluid=unique123 subject=Test\\sSubject message=";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Test Subject", "").toString());
	}

	@Test
	public void messageAdd_NullClientUIdException() {
		assertThrows(IllegalArgumentException.class, () -> MessageCommands.messageAdd(null, "Subject", "Message"));
	}

	@Test
	public void messageAdd_EmptyClientUIdException() {
		assertThrows(IllegalArgumentException.class, () -> MessageCommands.messageAdd("", "Subject", "Message"));
	}

	@Test
	public void messageAdd_SpecialCharactersInSubject() {
		String expected = "messageadd cluid=unique123 subject=Important\\s[URGENT]\\sMessage message=Content";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Important [URGENT] Message", "Content").toString());
	}

	@Test
	public void messageAdd_SpecialCharactersInMessage() {
		String expected = "messageadd cluid=unique123 subject=Subject message=Hello\\s\"World\"\\s&\\sGoodbye!";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Subject", "Hello \"World\" & Goodbye!").toString());
	}

	@Test
	public void messageAdd_LongContent() {
		String longSubject = "This is a very long subject line that contains multiple words and should be properly encoded";
		String longMessage = "This is a very long message content that spans multiple lines and contains various characters that need to be properly encoded when sent to the TeamSpeak server.";
		
		String result = MessageCommands.messageAdd("unique123", longSubject, longMessage).toString();
		assertTrue(result.startsWith("messageadd cluid=unique123"));
		assertTrue(result.contains("subject=This\\sis\\sa\\svery\\slong\\ssubject"));
		assertTrue(result.contains("message=This\\sis\\sa\\svery\\slong\\smessage"));
	}

	@Test
	public void messageDel_ValidMessageId() {
		String expected = "messagedel msgid=42";
		assertEquals(expected, MessageCommands.messageDel(42).toString());
	}

	@Test
	public void messageDel_ZeroMessageId() {
		String expected = "messagedel msgid=0";
		assertEquals(expected, MessageCommands.messageDel(0).toString());
	}

	@Test
	public void messageDel_NegativeMessageId() {
		String expected = "messagedel msgid=-1";
		assertEquals(expected, MessageCommands.messageDel(-1).toString());
	}

	@Test
	public void messageDel_LargeMessageId() {
		String expected = "messagedel msgid=999999";
		assertEquals(expected, MessageCommands.messageDel(999999).toString());
	}

	@Test
	public void messageGet_ValidMessageId() {
		String expected = "messageget msgid=123";
		assertEquals(expected, MessageCommands.messageGet(123).toString());
	}

	@Test
	public void messageGet_ZeroMessageId() {
		String expected = "messageget msgid=0";
		assertEquals(expected, MessageCommands.messageGet(0).toString());
	}

	@Test
	public void messageGet_NegativeMessageId() {
		String expected = "messageget msgid=-1";
		assertEquals(expected, MessageCommands.messageGet(-1).toString());
	}

	@Test
	public void messageGet_LargeMessageId() {
		String expected = "messageget msgid=888888";
		assertEquals(expected, MessageCommands.messageGet(888888).toString());
	}

	@Test
	public void messageList() {
		String expected = "messagelist";
		assertEquals(expected, MessageCommands.messageList().toString());
	}

	@Test
	public void messageUpdateFlag_MarkAsRead() {
		String expected = "messageupdateflag msgid=100 flag=1";
		assertEquals(expected, MessageCommands.messageUpdateFlag(100, true).toString());
	}

	@Test
	public void messageUpdateFlag_MarkAsUnread() {
		String expected = "messageupdateflag msgid=100 flag=0";
		assertEquals(expected, MessageCommands.messageUpdateFlag(100, false).toString());
	}

	@Test
	public void messageUpdateFlag_ZeroMessageId() {
		String expected = "messageupdateflag msgid=0 flag=1";
		assertEquals(expected, MessageCommands.messageUpdateFlag(0, true).toString());
	}

	@Test
	public void messageUpdateFlag_NegativeMessageId() {
		String expected = "messageupdateflag msgid=-1 flag=0";
		assertEquals(expected, MessageCommands.messageUpdateFlag(-1, false).toString());
	}

	@Test
	public void messageUpdateFlag_LargeMessageId() {
		String expected = "messageupdateflag msgid=777777 flag=1";
		assertEquals(expected, MessageCommands.messageUpdateFlag(777777, true).toString());
	}

	@Test
	public void messageAdd_AllNullExceptClientUId() {
		String expected = "messageadd cluid=unique456 subject= message=";
		assertEquals(expected, MessageCommands.messageAdd("unique456", null, null).toString());
	}

	@Test
	public void messageAdd_AllEmptyExceptClientUId() {
		String expected = "messageadd cluid=unique789 subject= message=";
		assertEquals(expected, MessageCommands.messageAdd("unique789", "", "").toString());
	}

	@Test
	public void messageAdd_ComplexClientUId() {
		String complexUId = "unique-id-with-dashes-and-numbers-123456789";
		String expected = "messageadd cluid=" + complexUId + " subject=Test message=Content";
		assertEquals(expected, MessageCommands.messageAdd(complexUId, "Test", "Content").toString());
	}

	@Test
	public void messageAdd_MultilineMessage() {
		String multilineMessage = "Line 1\nLine 2\nLine 3";
		String expected = "messageadd cluid=unique123 subject=Multiline message=Line\\s1\\nLine\\s2\\nLine\\s3";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Multiline", multilineMessage).toString());
	}

	@Test
	public void messageAdd_MessageWithTabs() {
		String messageWithTabs = "Column1\tColumn2\tColumn3";
		String expected = "messageadd cluid=unique123 subject=Tabbed message=Column1\\tColumn2\\tColumn3";
		assertEquals(expected, MessageCommands.messageAdd("unique123", "Tabbed", messageWithTabs).toString());
	}
}
