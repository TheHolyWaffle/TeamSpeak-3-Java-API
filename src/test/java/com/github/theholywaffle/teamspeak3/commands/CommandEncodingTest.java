package com.github.theholywaffle.teamspeak3.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandEncodingTest {

	@Test
	public void encode_NullString() {
		// CommandEncoding.encode doesn't handle null - it throws NPE
		assertThrows(NullPointerException.class, () -> CommandEncoding.encode(null));
	}

	@Test
	public void encode_EmptyString() {
		assertEquals("", CommandEncoding.encode(""));
	}

	@Test
	public void encode_SimpleString() {
		assertEquals("hello", CommandEncoding.encode("hello"));
	}

	@Test
	public void encode_StringWithSpaces() {
		assertEquals("hello\\sworld", CommandEncoding.encode("hello world"));
	}

	@Test
	public void encode_StringWithBackslashes() {
		assertEquals("hello\\\\world", CommandEncoding.encode("hello\\world"));
	}

	@Test
	public void encode_StringWithForwardSlashes() {
		assertEquals("hello\\/world", CommandEncoding.encode("hello/world"));
	}

	@Test
	public void encode_StringWithPipes() {
		assertEquals("hello\\pworld", CommandEncoding.encode("hello|world"));
	}

	@Test
	public void encode_StringWithNewlines() {
		assertEquals("hello\\nworld", CommandEncoding.encode("hello\nworld"));
	}

	@Test
	public void encode_StringWithCarriageReturns() {
		assertEquals("hello\\rworld", CommandEncoding.encode("hello\rworld"));
	}

	@Test
	public void encode_StringWithTabs() {
		assertEquals("hello\\tworld", CommandEncoding.encode("hello\tworld"));
	}

	@Test
	public void encode_StringWithVerticalTabs() {
		assertEquals("hello\\vworld", CommandEncoding.encode("hello\u000Bworld"));
	}

	@Test
	public void encode_StringWithFormFeeds() {
		assertEquals("hello\\fworld", CommandEncoding.encode("hello\fworld"));
	}

	@Test
	public void encode_StringWithBells() {
		assertEquals("hello\\aworld", CommandEncoding.encode("hello\u0007world"));
	}

	@Test
	public void encode_StringWithBackspaces() {
		assertEquals("hello\\bworld", CommandEncoding.encode("hello\bworld"));
	}

	@Test
	public void encode_AllSpecialCharacters() {
		String input = "test \\ / | \n \r \t \u000B \f \u0007 \b end";
		String expected = "test\\s\\\\\\s\\/\\s\\p\\s\\n\\s\\r\\s\\t\\s\\v\\s\\f\\s\\a\\s\\b\\send";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_StringWithQuotes() {
		// Quotes should NOT be encoded
		assertEquals("hello\"world\"", CommandEncoding.encode("hello\"world\""));
	}

	@Test
	public void encode_StringWithSquareBrackets() {
		// Square brackets should NOT be encoded
		assertEquals("hello[world]", CommandEncoding.encode("hello[world]"));
	}

	@Test
	public void encode_StringWithAmpersands() {
		// Ampersands should NOT be encoded
		assertEquals("hello&world", CommandEncoding.encode("hello&world"));
	}

	@Test
	public void encode_StringWithNumbers() {
		assertEquals("test123", CommandEncoding.encode("test123"));
	}

	@Test
	public void encode_StringWithSpecialSymbols() {
		// These symbols should NOT be encoded
		assertEquals("test@#$%^*()_+-={}[]:\";'<>?,.", CommandEncoding.encode("test@#$%^*()_+-={}[]:\";'<>?,."));
	}

	@Test
	public void encode_LongString() {
		String input = "This is a very long string with spaces and special characters like / and | and \\ and newlines\n";
		String expected = "This\\sis\\sa\\svery\\slong\\sstring\\swith\\sspaces\\sand\\sspecial\\scharacters\\slike\\s\\/\\sand\\s\\p\\sand\\s\\\\\\sand\\snewlines\\n";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_UnicodeCharacters() {
		// Unicode characters should be preserved, but spaces are still encoded
		assertEquals("héllo\\swörld", CommandEncoding.encode("héllo wörld"));
	}

	@Test
	public void encode_EmojiCharacters() {
		// Emoji characters should be preserved (but spaces encoded)
		assertEquals("hello\\s😀\\sworld", CommandEncoding.encode("hello 😀 world"));
	}

	@Test
	public void encode_OnlySpaces() {
		assertEquals("\\s\\s\\s", CommandEncoding.encode("   "));
	}

	@Test
	public void encode_OnlySpecialCharacters() {
		assertEquals("\\\\\\s\\/\\s\\p\\s\\n\\s\\r\\s\\t", CommandEncoding.encode("\\ / | \n \r \t"));
	}

	@Test
	public void encode_MixedContent() {
		String input = "User: John Doe\nChannel: General/Chat\nReason: Spamming|Trolling";
		String expected = "User:\\sJohn\\sDoe\\nChannel:\\sGeneral\\/Chat\\nReason:\\sSpamming\\pTrolling";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_PathLikeString() {
		assertEquals("C:\\\\Users\\\\John\\sDoe\\/Documents\\/file.txt", 
			CommandEncoding.encode("C:\\Users\\John Doe/Documents/file.txt"));
	}

	@Test
	public void encode_CommandLikeString() {
		assertEquals("clientkick\\sreason=Bad\\sbehavior\\s\\p\\sSpamming",
			CommandEncoding.encode("clientkick reason=Bad behavior | Spamming"));
	}

	@Test
	public void encode_MultilineText() {
		String input = "Line 1\nLine 2\rLine 3\r\nLine 4";
		String expected = "Line\\s1\\nLine\\s2\\rLine\\s3\\r\\nLine\\s4";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_TabSeparatedValues() {
		String input = "Name\tValue\tDescription";
		String expected = "Name\\tValue\\tDescription";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_ControlCharacters() {
		// Test various control characters
		String input = "\u0001\u0002\u0003\u0004\u0005\u0006"; // SOH, STX, ETX, EOT, ENQ, ACK
		// These should be preserved as-is since they're not in the encoding map
		assertEquals(input, CommandEncoding.encode(input));
	}

	@Test
	public void encode_EdgeCaseCharacters() {
		// Test specific control characters that are encoded
		assertEquals("\\a", CommandEncoding.encode("\u0007")); // BEL
		assertEquals("\\b", CommandEncoding.encode("\u0008")); // BS
		assertEquals("\\t", CommandEncoding.encode(String.valueOf((char) 0x0009))); // TAB
		assertEquals("\\n", CommandEncoding.encode(String.valueOf((char) 0x000A))); // LF
		assertEquals("\\v", CommandEncoding.encode(String.valueOf((char) 0x000B))); // VT
		assertEquals("\\f", CommandEncoding.encode(String.valueOf((char) 0x000C))); // FF
		assertEquals("\\r", CommandEncoding.encode(String.valueOf((char) 0x000D))); // CR
	}

	@Test
	public void encode_RepeatedSpecialCharacters() {
		assertEquals("\\s\\s\\s\\\\\\\\\\\\", CommandEncoding.encode("   \\\\\\"));
	}

	@Test
	public void encode_AlternatingSpecialCharacters() {
		assertEquals("a\\sb\\\\c\\/d\\pe\\nf", CommandEncoding.encode("a b\\c/d|e\nf"));
	}

	@Test
	public void encode_StartingWithSpecialCharacter() {
		assertEquals("\\sstart", CommandEncoding.encode(" start"));
	}

	@Test
	public void encode_EndingWithSpecialCharacter() {
		assertEquals("end\\s", CommandEncoding.encode("end "));
	}

	@Test
	public void encode_OnlyEncodableCharacters() {
		String input = " \\ / | \n \r \t \u000B \f \u0007 \b";
		String expected = "\\s\\\\\\s\\/\\s\\p\\s\\n\\s\\r\\s\\t\\s\\v\\s\\f\\s\\a\\s\\b";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_RealWorldExample() {
		String input = "User \"Admin\" kicked from channel \"General Chat\" | Reason: Inappropriate behavior\nDuration: 5 minutes";
		String expected = "User\\s\"Admin\"\\skicked\\sfrom\\schannel\\s\"General\\sChat\"\\s\\p\\sReason:\\sInappropriate\\sbehavior\\nDuration:\\s5\\sminutes";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_TeamSpeakServerResponse() {
		String input = "error id=0 msg=ok\nclid=1 client_nickname=ServerAdmin";
		String expected = "error\\sid=0\\smsg=ok\\nclid=1\\sclient_nickname=ServerAdmin";
		assertEquals(expected, CommandEncoding.encode(input));
	}

	@Test
	public void encode_ConsistencyCheck() {
		// Encoding the same string multiple times should yield the same result
		String input = "test string with spaces and / slashes";
		String encoded1 = CommandEncoding.encode(input);
		String encoded2 = CommandEncoding.encode(input);
		assertEquals(encoded1, encoded2);
	}
}
