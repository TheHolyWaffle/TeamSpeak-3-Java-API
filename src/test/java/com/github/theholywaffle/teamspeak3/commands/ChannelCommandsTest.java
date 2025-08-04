package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelCommandsTest {

	@Test
	public void channelCreate_BasicName() {
		String expected = "channelcreate channel_name=TestChannel";
		assertEquals(expected, ChannelCommands.channelCreate("TestChannel", null).toString());
	}

	@Test
	public void channelCreate_WithOptions() {
		Map<ChannelProperty, String> options = new HashMap<>();
		options.put(ChannelProperty.CHANNEL_TOPIC, "Test Topic");
		options.put(ChannelProperty.CHANNEL_MAXCLIENTS, "10");
		
		String result = ChannelCommands.channelCreate("TestChannel", options).toString();
		assertTrue(result.startsWith("channelcreate channel_name=TestChannel"));
		assertTrue(result.contains("channel_topic=Test\\sTopic"));
		assertTrue(result.contains("channel_maxclients=10"));
	}

	@Test
	public void channelCreate_EmptyOptions() {
		Map<ChannelProperty, String> options = new HashMap<>();
		String expected = "channelcreate channel_name=TestChannel";
		assertEquals(expected, ChannelCommands.channelCreate("TestChannel", options).toString());
	}

	@Test
	public void channelCreate_NullNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelCommands.channelCreate(null, null));
	}

	@Test
	public void channelCreate_EmptyNameException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelCommands.channelCreate("", null));
	}

	@Test
	public void channelCreate_SpecialCharactersInName() {
		String expected = "channelcreate channel_name=Test\\sChannel\\s[VIP]";
		assertEquals(expected, ChannelCommands.channelCreate("Test Channel [VIP]", null).toString());
	}

	@Test
	public void channelDelete_WithoutForce() {
		String expected = "channeldelete cid=5 force=0";
		assertEquals(expected, ChannelCommands.channelDelete(5, false).toString());
	}

	@Test
	public void channelDelete_WithForce() {
		String expected = "channeldelete cid=10 force=1";
		assertEquals(expected, ChannelCommands.channelDelete(10, true).toString());
	}

	@Test
	public void channelDelete_ZeroChannelId() {
		String expected = "channeldelete cid=0 force=0";
		assertEquals(expected, ChannelCommands.channelDelete(0, false).toString());
	}

	@Test
	public void channelDelete_NegativeChannelId() {
		String expected = "channeldelete cid=-1 force=1";
		assertEquals(expected, ChannelCommands.channelDelete(-1, true).toString());
	}

	@Test
	public void channelEdit_WithOptions() {
		Map<ChannelProperty, String> options = new HashMap<>();
		options.put(ChannelProperty.CHANNEL_NAME, "New Name");
		options.put(ChannelProperty.CHANNEL_TOPIC, "New Topic");
		
		String result = ChannelCommands.channelEdit(5, options).toString();
		assertTrue(result.startsWith("channeledit cid=5"));
		assertTrue(result.contains("channel_name=New\\sName"));
		assertTrue(result.contains("channel_topic=New\\sTopic"));
	}

	@Test
	public void channelEdit_EmptyOptions() {
		Map<ChannelProperty, String> options = new HashMap<>();
		String expected = "channeledit cid=5";
		assertEquals(expected, ChannelCommands.channelEdit(5, options).toString());
	}

	@Test
	public void channelEdit_NullOptions() {
		String expected = "channeledit cid=5";
		assertEquals(expected, ChannelCommands.channelEdit(5, null).toString());
	}

	@Test
	public void channelFind_ValidPattern() {
		String expected = "channelfind pattern=Test";
		assertEquals(expected, ChannelCommands.channelFind("Test").toString());
	}

	@Test
	public void channelFind_PatternWithSpaces() {
		String expected = "channelfind pattern=Test\\sChannel";
		assertEquals(expected, ChannelCommands.channelFind("Test Channel").toString());
	}

	@Test
	public void channelFind_PatternWithWildcards() {
		String expected = "channelfind pattern=Test*";
		assertEquals(expected, ChannelCommands.channelFind("Test*").toString());
	}

	@Test
	public void channelFind_NullPatternException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelCommands.channelFind(null));
	}

	@Test
	public void channelFind_EmptyPatternException() {
		assertThrows(IllegalArgumentException.class, () -> ChannelCommands.channelFind(""));
	}

	@Test
	public void channelInfo_ValidChannelId() {
		String expected = "channelinfo cid=5";
		assertEquals(expected, ChannelCommands.channelInfo(5).toString());
	}

	@Test
	public void channelInfo_ZeroChannelId() {
		String expected = "channelinfo cid=0";
		assertEquals(expected, ChannelCommands.channelInfo(0).toString());
	}

	@Test
	public void channelInfo_NegativeChannelId() {
		String expected = "channelinfo cid=-1";
		assertEquals(expected, ChannelCommands.channelInfo(-1).toString());
	}

	@Test
	public void channelList_AllOptions() {
		String expected = "channellist -topic -flags -voice -limits -icon -secondsempty -banners";
		assertEquals(expected, ChannelCommands.channelList().toString());
	}

	@Test
	public void channelMove_ValidParameters() {
		String expected = "channelmove cid=5 cpid=1 order=2";
		assertEquals(expected, ChannelCommands.channelMove(5, 1, 2).toString());
	}

	@Test
	public void channelMove_ZeroOrder() {
		String expected = "channelmove cid=5 cpid=1 order=0";
		assertEquals(expected, ChannelCommands.channelMove(5, 1, 0).toString());
	}

	@Test
	public void channelMove_NegativeOrder() {
		// Negative order should be converted to 0
		String expected = "channelmove cid=5 cpid=1 order=0";
		assertEquals(expected, ChannelCommands.channelMove(5, 1, -1).toString());
	}

	@Test
	public void channelMove_NegativeChannelId() {
		String expected = "channelmove cid=-1 cpid=0 order=0";
		assertEquals(expected, ChannelCommands.channelMove(-1, 0, 0).toString());
	}

	@Test
	public void channelMove_NegativeParentId() {
		String expected = "channelmove cid=5 cpid=-1 order=0";
		assertEquals(expected, ChannelCommands.channelMove(5, -1, 0).toString());
	}

	@Test
	public void channelCreate_MultipleOptions() {
		Map<ChannelProperty, String> options = new HashMap<>();
		options.put(ChannelProperty.CHANNEL_TOPIC, "Test Topic");
		options.put(ChannelProperty.CHANNEL_MAXCLIENTS, "50");
		options.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
		options.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "7");
		
		String result = ChannelCommands.channelCreate("TestChannel", options).toString();
		assertTrue(result.startsWith("channelcreate channel_name=TestChannel"));
		assertTrue(result.contains("channel_topic=Test\\sTopic"));
		assertTrue(result.contains("channel_maxclients=50"));
		assertTrue(result.contains("channel_flag_permanent=1"));
		assertTrue(result.contains("channel_codec_quality=7"));
	}

	@Test
	public void channelEdit_SingleOption() {
		Map<ChannelProperty, String> options = new HashMap<>();
		options.put(ChannelProperty.CHANNEL_MAXCLIENTS, "25");
		
		String expected = "channeledit cid=10 channel_maxclients=25";
		assertEquals(expected, ChannelCommands.channelEdit(10, options).toString());
	}

	@Test
	public void channelFind_SpecialCharacters() {
		String expected = "channelfind pattern=[Admin]\\sChannel";
		assertEquals(expected, ChannelCommands.channelFind("[Admin] Channel").toString());
	}
}
