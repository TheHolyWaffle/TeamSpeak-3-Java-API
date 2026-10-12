package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandSyntaxTest {

	@Test
	void parametersEncodeTheirValues() {
		assertEquals("key=a\\sb", new KeyValueParam("key", "a b").encoded());
		assertEquals("cpw=", new KeyValueParam("cpw", (String) null).encoded());
		assertEquals("flag=1 count=-2 size=8589934592", new CommandBuilder("x")
			.add(new KeyValueParam("flag", true)).add(new KeyValueParam("count", -2))
			.add(new KeyValueParam("size", 8_589_934_592L)).build().toString().substring(2));
		assertEquals("-continueonerror", new OptionParam("continueonerror").encoded());
		assertEquals("a\\pb", new ValueParam("a|b").encoded());
		assertEquals("already\\sencoded|raw", new RawParam("already\\sencoded|raw").encoded());
		assertThrows(IllegalArgumentException.class, () -> new KeyValueParam(null, "value"));
	}

	@Test
	void arrayEntriesArePipeSeparatedAndImmutable() {
		List<Parameter> first = new ArrayList<>(List.of(new KeyValueParam("cid", 1), new KeyValueParam("name", "/a b")));
		var array = new ArrayParameter(List.of(first, List.of(new KeyValueParam("cid", 2))));
		first.clear();
		assertEquals("cid=1 name=\\/a\\sb|cid=2", array.encoded());
		assertEquals("clid=3|clid=4", ArrayParameter.of("clid", new int[] {3, 4}).encoded());
		assertEquals(ArrayParameter.of("clid", List.of("3")), ArrayParameter.of("clid", new int[] {3}));
	}

	@Test
	void commandsJoinNameAndParameters() {
		assertEquals("banclient banreason=bye -continueonerror clid=3|clid=4",
			BanCommands.banClient(new int[] {3, 4}, 0, "bye", true).toString());
		assertEquals("permidgetbyname permsid=a|permsid=b", PermissionCommands.permIdGetByName("a", "b").toString());
		assertEquals("permidgetbyname permsid=a", PermissionCommands.permIdGetByName("a").toString());
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet("a", null));
		assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet());
		assertEquals("channelmove cid=1 cpid=0 order=0", ChannelCommands.channelMove(1, 0, -5).toString());
	}

	@Test
	void propertiesKeepMapOrderAndRejectReadOnlyProperties() {
		Map<ChannelProperty, String> properties = new LinkedHashMap<>();
		properties.put(ChannelProperty.CHANNEL_TOPIC, "a topic");
		properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
		assertEquals("channeledit cid=5 channel_topic=a\\stopic channel_flag_permanent=1",
			ChannelCommands.channelEdit(5, properties).toString());
		assertEquals("channeledit cid=5", ChannelCommands.channelEdit(5, Map.of()).toString());
		assertThrows(IllegalArgumentException.class,
			() -> ChannelCommands.channelEdit(5, Map.of(ChannelProperty.CID, "1")));
	}

	@Test
	void singlePassEncodingMatchesSequentialReplacement() {
		var random = new Random(445);
		char[] alphabet = "\\ /|\b\f\n\r\t\u0007\u000bsp|ab=\u00e9".toCharArray();
		for (int run = 0; run < 2_000; run++) {
			var value = new StringBuilder();
			for (int i = random.nextInt(12); i > 0; i--) value.append(alphabet[random.nextInt(alphabet.length)]);
			String plain = value.toString();
			assertEquals(sequentialReplacement(plain), CommandEncoding.encode(plain), plain);
		}
	}

	private static String sequentialReplacement(String str) {
		return str.replace("\\", "\\\\").replace(" ", "\\s").replace("/", "\\/").replace("|", "\\p")
			.replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r")
			.replace("\t", "\\t").replace("\u0007", "\\a").replace("\u000b", "\\v");
	}
}
