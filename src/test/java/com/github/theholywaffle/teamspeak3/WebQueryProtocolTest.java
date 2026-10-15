package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.exception.TS3AuthenticationFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3HttpException;
import com.github.theholywaffle.teamspeak3.commands.ChannelCommands;
import com.github.theholywaffle.teamspeak3.commands.ClientCommands;
import com.github.theholywaffle.teamspeak3.commands.FileCommands;
import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import com.github.theholywaffle.teamspeak3.commands.ServerCommands;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebQueryProtocolTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "use",
                "login",
                "logout",
                "quit",
                "help",
                "servernotifyregister",
                "servernotifyunregister",
                "ftgetfilelist",
                "ftinitupload",
                "ftcreatedir"
            })
    void rejectsCommandsTheServerDocumentsAsUnsupported(String command) {
        var reason = WebQueryProtocol.unsupported(QueryCommands.rawCommand(command));
        assertTrue(reason.orElseThrow().startsWith(command + " is unsupported: "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"version", "whoami", "channellist", "clientupdate", "apikeyadd", "serveredit", "f"})
    void sendsEverythingElse(String command) {
        assertEquals(Optional.empty(), WebQueryProtocol.unsupported(QueryCommands.rawCommand(command)));
    }

    @Test
    void decidesFromTraitsForBuiltCommandsToo() {
        for (var command : List.of(
                QueryCommands.useId(VirtualServerId.of(1), null),
                QueryCommands.logIn("serveradmin", "secret"),
                QueryCommands.quit(),
                QueryCommands.serverNotifyRegister(
                        com.github.theholywaffle.teamspeak3.api.event.TS3EventType.SERVER, java.util.Optional.empty()),
                FileCommands.ftList())) {
            assertTrue(WebQueryProtocol.unsupported(command).isPresent(), command.getName());
        }
        assertEquals(Optional.empty(), WebQueryProtocol.unsupported(ClientCommands.clientUpdate(Map.of())));
    }

    @Test
    void targetsInstanceServerAndPortScopesBelowTheBaseUri() {
        URI root = URI.create("https://ts.example.org:10443/");
        assertEquals(URI.create("https://ts.example.org:10443/version"), WebQueryProtocol.target(root, "", "version"));
        assertEquals(
                URI.create("https://ts.example.org:10443/7/channellist"),
                WebQueryProtocol.target(root, "7/", "channellist"));
        URI proxied = URI.create("https://proxy.example.org/teamspeak/");
        assertEquals(
                URI.create("https://proxy.example.org/teamspeak/byport/9987/clientlist"),
                WebQueryProtocol.target(proxied, "byport/9987/", "clientlist"));
    }

    @Test
    void encodesParametersAsOneObjectWithDecodedValuesAndOptions() {
        var properties = new LinkedHashMap<ChannelProperty, String>();
        properties.put(ChannelProperty.CHANNEL_TOPIC, "a b|c \\s \"q\" é\n");
        assertEquals(
                "{\"channel_name\":\"name with space\",\"channel_topic\":\"a b|c \\\\s \\\"q\\\" é\\n\"}",
                WebQueryProtocol.body(ChannelCommands.channelCreate("name with space", properties)));
        assertEquals(
                "{\"-topic\":\"\",\"-flags\":\"\",\"-voice\":\"\",\"-limits\":\"\",\"-icon\":\"\","
                        + "\"-secondsempty\":\"\",\"-banners\":\"\"}",
                WebQueryProtocol.body(ChannelCommands.channelList()));
        assertEquals("{}", WebQueryProtocol.body(ServerCommands.version()));
    }

    @Test
    void encodesPipeEntriesAsAnArrayWhoseFirstObjectHoldsSharedParameters() {
        assertEquals(
                "[{\"reasonid\":\"5\",\"reasonmsg\":\"bye now\",\"clid\":\"1\"},{\"clid\":\"2\"},{\"clid\":\"3\"}]",
                WebQueryProtocol.body(
                        ClientCommands.clientKick(ReasonIdentifier.REASON_KICK_SERVER, "bye now", 1, 2, 3)));
        assertEquals(
                "[{\"sgid\":\"6\",\"permsid\":\"a\",\"permvalue\":\"1\"},{\"permsid\":\"b\",\"permvalue\":\"2\"}]",
                WebQueryProtocol.body(
                        QueryCommands.rawCommand("x sgid=6 permsid=a permvalue=1|permsid=b permvalue=2")));
        assertEquals("{\"a\":\"\"}", WebQueryProtocol.body(QueryCommands.rawCommand("x  a= ")));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{",
                "{\"status\":{\"code\":0}",
                "{\"status\":{\"code\":0},}",
                "{status:{code:0}}",
                "{\"status\":{\"code\":0}} {}",
                "{\"status\":{\"code\":0},\"body\":[{\"a\":\"1\",\"a\":\"2\"}]}",
                "{\"status\":{\"code\":0},\"body\":[null]}",
                "'{}'",
                "<html>"
            })
    void rejectsMalformedOrAmbiguousJson(String body) {
        var error = assertThrows(TS3HttpException.class, () -> WebQueryProtocol.response("version", 200, body));
        assertTrue(error.isOutcomeUnknown());
    }

    @Test
    void decodesJsonEscapesAndSkipsUnknownMembers() {
        var response = WebQueryProtocol.response(
                "x",
                200,
                "{\"extra\":[[{\"a\":1}]],\"status\":{\"code\":0,\"message\":\"ok\"},\"body\":[{\"v\":"
                        + "\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u00e9 \\ud83d\\ude00\",\"n\":12,\"z\":null}]}");
        var row = response.getFirstResponse().orElseThrow();
        assertEquals("q\" b\\ s/ \b\f\n\r\t é 😀", row.require("v"));
        assertEquals("12", row.require("n"));
        assertEquals("", row.require("z"));
    }

    @Test
    void boundsNestingInsteadOfRecursing() {
        String deep = "[".repeat(100_000) + "]".repeat(100_000);
        var error = assertThrows(
                TS3HttpException.class,
                () -> WebQueryProtocol.response("x", 200, "{\"extra\":" + deep + ",\"status\":{\"code\":0}}"));
        assertTrue(error.isOutcomeUnknown());
    }

    @Test
    void encodesControlCharactersAndUnicodeAsValidJson() {
        String value = "\b\f\n\r\t\u0001\u001f q\"\\/ é😀 <tag> & =";
        String body = WebQueryProtocol.body(ChannelCommands.channelFind(value));
        assertTrue(body.chars().noneMatch(c -> c < 0x20), body);
        assertTrue(body.contains("<tag> & ="), "no HTML escaping: " + body);
        assertEquals(
                value,
                JsonParser.parseString(body).getAsJsonObject().get("pattern").getAsString());
    }

    @Test
    void mapsRowsLikeAServerQueryResponse() {
        var response = WebQueryProtocol.response(
                "logview",
                200,
                "{\"body\":[{\"last_pos\":\"9\",\"l\":\"one | two \\\\s\"},{\"l\":\"three\"}],"
                        + "\"status\":{\"code\":0,\"message\":\"ok\"}}");
        assertEquals(2, response.getResponses().size());
        assertEquals("one | two \\s", response.getResponses().get(0).require("l"));
        assertEquals("three", response.getResponses().get(1).require("l"));
        assertEquals("9", response.getResponses().get(1).require("last_pos"), "shared fields are inherited");
        assertEquals("last_pos=9 l=one\\s\\p\\stwo\\s\\\\s|l=three", response.getRawResponse());
    }

    @Test
    void redactsSecretsLikeServerQuery() {
        var response = WebQueryProtocol.response(
                "apikeyadd",
                200,
                "{\"body\":[{\"apikey\":\"BAsecret\",\"id\":\"3\"}],\"status\":{\"code\":0,\"message\":\"ok\"}}");
        assertEquals("BAsecret", response.getResponses().getFirst().require("apikey"));
        assertFalse(response.toString().contains("BAsecret"));
    }

    @Test
    void treatsEmptyResultsAsSuccess() {
        assertEquals(
                List.of(),
                WebQueryProtocol.response(
                                "banlist",
                                200,
                                "{\"status\":{\"code\":1281,\"message\":\"database empty result set\"}}")
                        .getResponses());
        assertEquals(
                List.of(),
                WebQueryProtocol.response("servergroupclientlist", 200, "{\"status\":{\"code\":0}}")
                        .getResponses());
    }

    @Test
    void translatesServerErrorsWithTheirDetails() {
        var error = assertThrows(
                TS3CommandFailedException.class,
                () -> WebQueryProtocol.response(
                        "channelinfo",
                        200,
                        "{\"status\":{\"code\":2568,\"message\":\"insufficient client permissions\","
                                + "\"extra_message\":\"x\",\"failed_permid\":\"12\"}}"));
        assertEquals(2568, error.getErrorId());
        assertEquals("channelinfo", error.getCommandName());
        assertEquals(Optional.of("x"), error.getError().getExtraMessage());
        assertEquals(12, error.getError().getFailedPermissionId().orElseThrow());
        assertFalse(error.isOutcomeUnknown());
        var badRequest = assertThrows(
                TS3CommandFailedException.class,
                () -> WebQueryProtocol.response(
                        "nosuch",
                        400,
                        "{\"status\":{\"code\":1538,\"extra_message\":\"unknown command\",\"message\":\"invalid parameter\"}}"));
        assertEquals(1538, badRequest.getErrorId());
        assertEquals(TS3CommandFailedException.class, badRequest.getClass());
    }

    @Test
    void separatesAuthenticationFailures() {
        for (int status : new int[] {401, 403}) {
            var error = assertThrows(
                    TS3AuthenticationFailedException.class,
                    () -> WebQueryProtocol.response(
                            "version",
                            status,
                            "{\"status\":{\"code\":5122,\"extra_message\":\"invalid api key\",\"message\":\"invalid apikey\"}}"));
            assertEquals(5122, error.getErrorId());
            assertTrue(error.getMessage().contains("invalid apikey"));
        }
    }

    @Test
    void reportsUnusableResponsesWithStatusAndOutcome() {
        Map<Integer, String> bodies = Map.of(
                502, "<html>Bad Gateway</html>",
                404, "",
                200, "{\"body\":[]}",
                201, "{\"status\":{\"code\":\"x\"}}",
                202, "{\"status\":[]}",
                203, "{\"status\":{\"code\":0},\"body\":{}}",
                204, "{\"status\":{\"code\":0},\"body\":[1]}",
                205, "{\"status\":{\"code\":0},\"body\":[{\"a\":[]}]}",
                206, "[]");
        bodies.forEach((status, body) -> {
            var error = assertThrows(
                    TS3HttpException.class, () -> WebQueryProtocol.response("channellist", status, body), body);
            assertEquals(status, error.getStatusCode());
            assertEquals("channellist", error.getCommandName());
            assertEquals(status != 404, error.isOutcomeUnknown(), body);
            assertTrue(error.getMessage().startsWith("Web Query command channellist returned HTTP " + status));
        });
        var inconsistent = assertThrows(
                TS3HttpException.class,
                () -> WebQueryProtocol.response("version", 500, "{\"status\":{\"code\":0,\"message\":\"ok\"}}"));
        assertTrue(inconsistent.isOutcomeUnknown());
        assertFalse(assertThrows(
                        TS3HttpException.class,
                        () -> WebQueryProtocol.response("version", 499, "{\"status\":{\"code\":0}}"))
                .isOutcomeUnknown());
    }
}
