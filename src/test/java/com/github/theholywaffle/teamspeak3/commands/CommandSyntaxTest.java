package com.github.theholywaffle.teamspeak3.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupFilter;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyGrant;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CommandSyntaxTest {

    @Test
    void parametersEncodeTheirValues() {
        assertEquals("key=a\\sb", new KeyValueParam("key", "a b").encoded());
        assertEquals("cpw=", new KeyValueParam("cpw", (String) null).encoded());
        assertEquals(
                "x flag=1 count=-2 size=8589934592",
                new CommandBuilder("x")
                        .add(new KeyValueParam("flag", true))
                        .add(new KeyValueParam("count", -2))
                        .add(new KeyValueParam("size", 8_589_934_592L))
                        .build()
                        .encoded());
        assertEquals("-continueonerror", new OptionParam("continueonerror").encoded());
        assertEquals("a\\pb", new ValueParam("a|b").encoded());
        assertEquals("already\\sencoded|raw", new RawParam("already\\sencoded|raw").encoded());
        assertThrows(IllegalArgumentException.class, () -> new KeyValueParam(null, "value"));
    }

    @Test
    void arrayEntriesArePipeSeparatedAndImmutable() {
        List<Parameter> first =
                new ArrayList<>(List.of(new KeyValueParam("cid", 1), new KeyValueParam("name", "/a b")));
        var array = new ArrayParameter(List.of(first, List.of(new KeyValueParam("cid", 2))));
        first.clear();
        assertEquals("cid=1 name=\\/a\\sb|cid=2", array.encoded());
        assertEquals(
                "clid=3|clid=4", ArrayParameter.of("clid", new int[] {3, 4}).encoded());
        assertEquals(ArrayParameter.of("clid", List.of("3")), ArrayParameter.of("clid", new int[] {3}));
    }

    @Test
    void commandsJoinNameAndParameters() {
        assertEquals(
                "banclient banreason=bye -continueonerror clid=3|clid=4",
                BanCommands.banClient(new int[] {3, 4}, 0, "bye", true).encoded());
        assertEquals(
                "permidgetbyname permsid=a|permsid=b",
                PermissionCommands.permIdGetByName("a", "b").encoded());
        assertEquals(
                "permidgetbyname permsid=a",
                PermissionCommands.permIdGetByName("a").encoded());
        assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet("a", null));
        assertThrows(IllegalArgumentException.class, () -> PermissionCommands.permGet());
        assertEquals(
                "channelmove cid=1 cpid=0 order=0",
                ChannelCommands.channelMove(ChannelId.of(1), Optional.empty(), Optional.empty())
                        .encoded());
        assertEquals(
                "channelmove cid=1 cpid=2 order=3",
                ChannelCommands.channelMove(ChannelId.of(1), Optional.of(ChannelId.of(2)), Optional.of(ChannelId.of(3)))
                        .encoded());
        assertEquals(
                "servernotifyregister event=channel id=0",
                QueryCommands.serverNotifyRegister(TS3EventType.CHANNEL, Optional.empty())
                        .encoded());
        assertEquals(
                "servernotifyregister event=server",
                QueryCommands.serverNotifyRegister(TS3EventType.SERVER, Optional.empty())
                        .encoded());
        assertEquals(
                "servernotifyregister event=server id=4",
                QueryCommands.serverNotifyRegister(TS3EventType.SERVER, Optional.of(ChannelId.of(4)))
                        .encoded());
        assertEquals(
                "complainlist", ComplaintCommands.complainList(Optional.empty()).encoded());
        assertEquals(
                "channelgroupclientlist cldbid=3",
                ChannelGroupCommands.channelGroupClientList(
                                ChannelGroupFilter.all().ofClient(ClientDatabaseId.of(3)))
                        .encoded());
        assertEquals(
                "privilegekeyadd tokentype=0 tokenid1=6 tokenid2=0",
                PrivilegeKeyCommands.privilegeKeyAdd(PrivilegeKeyGrant.serverGroup(ServerGroupId.of(6)), null)
                        .encoded());
    }

    @Test
    void propertiesKeepMapOrderAndRejectReadOnlyProperties() {
        Map<ChannelProperty, String> properties = new LinkedHashMap<>();
        properties.put(ChannelProperty.CHANNEL_TOPIC, "a topic");
        properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
        assertEquals(
                "channeledit cid=5 channel_topic=a\\stopic channel_flag_permanent=1",
                ChannelCommands.channelEdit(ChannelId.of(5), properties).encoded());
        assertEquals(
                "channeledit cid=5",
                ChannelCommands.channelEdit(ChannelId.of(5), Map.of()).encoded());
        assertThrows(
                IllegalArgumentException.class,
                () -> ChannelCommands.channelEdit(ChannelId.of(5), Map.of(ChannelProperty.CID, "1")));
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
        return str.replace("\\", "\\\\")
                .replace(" ", "\\s")
                .replace("/", "\\/")
                .replace("|", "\\p")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\u0007", "\\a")
                .replace("\u000b", "\\v");
    }
}
