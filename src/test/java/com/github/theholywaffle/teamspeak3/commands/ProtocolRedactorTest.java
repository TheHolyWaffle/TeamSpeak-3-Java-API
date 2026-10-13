package com.github.theholywaffle.teamspeak3.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProtocolRedactorTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '#',
            value = {
                // Positional and keyed login; the user name stays visible.
                "login serveradmin s3cret# login serveradmin <redacted>",
                "login client_login_name=admin client_login_password=s3cret"
                        + "# login client_login_name=admin client_login_password=<redacted>",
                // Channel and file passwords, including a rename's target password.
                "clientmove clid=5 cid=2 cpw=chan\\spw# clientmove clid=5 cid=2 cpw=<redacted>",
                "ftrenamefile cid=1 cpw=a tcid=2 tcpw=b oldname=/x newname=/y"
                        + "# ftrenamefile cid=1 cpw=<redacted> tcid=2 tcpw=<redacted> oldname=/x newname=/y",
                "serveredit virtualserver_password=pw virtualserver_name=Main"
                        + "# serveredit virtualserver_password=<redacted> virtualserver_name=Main",
                "channelcreate channel_name=Lobby channel_password=pw"
                        + "# channelcreate channel_name=Lobby channel_password=<redacted>",
                "serversnapshotdeploy -mapping password=snap data=abc"
                        + "# serversnapshotdeploy -mapping password=<redacted> data=abc",
                // Responses: transfer keys, every privilege key row, temporary passwords and API keys.
                "clientftfid=1 serverftfid=2 ftkey=K3y port=30033 size=0"
                        + "# clientftfid=1 serverftfid=2 ftkey=<redacted> port=30033 size=0",
                "token=aaa token_type=0 token_description=lobby|token=bbb token_type=1 token_description="
                        + "# token=<redacted> token_type=0 token_description=lobby|token=<redacted> token_type=1"
                        + " token_description=",
                "nickname=admin uid=x desc=d pw_clear=temp start=1 end=2 tcid=0"
                        + "# nickname=admin uid=x desc=d pw_clear=<redacted> start=1 end=2 tcid=0",
                "apikey=abc id=1 sid=1 cldbid=1 scope=manage time_left=unlimited"
                        + "# apikey=<redacted> id=1 sid=1 cldbid=1 scope=manage time_left=unlimited",
                "client_login_name=bot client_login_password=Gen3rated"
                        + "# client_login_name=bot client_login_password=<redacted>",
                // Notifications and errors keep their structure.
                "notifytokenused clid=5 cldbid=3 cluid=u token=aaa tokencustomset= token1=7 token2=0"
                        + "# notifytokenused clid=5 cldbid=3 cluid=u token=<redacted> tokencustomset= token1=7 token2=0",
                "error id=0 msg=ok# error id=0 msg=ok",
                // Escaped separators inside a secret are part of the value.
                "tokenuse token=a\\sb\\pc# tokenuse token=<redacted>",
                "version# version",
                "use sid=1 -virtual# use sid=1 -virtual"
            })
    void redactsSecretValuesAndKeepsStructure(String line, String expected) {
        assertEquals(expected.strip(), ProtocolRedactor.redactLine(line.strip()));
    }

    @Test
    void emptySecretsStayEmpty() {
        assertEquals(
                "channeledit cid=1 channel_password=",
                ProtocolRedactor.redactLine("channeledit cid=1 channel_password="));
    }

    @Test
    void classifiesSecretKeysWithoutCatchingSimilarNames() {
        for (String key :
                new String[] {"cpw", "TCPW", "token", "ftkey", "apikey", "pw_clear", "virtualserver_password"}) {
            assertTrue(ProtocolRedactor.isSecret(key), key);
        }
        for (String key : new String[] {
            "token_type",
            "token1",
            "tokencustomset",
            "key",
            "cid",
            "channel_flag_password",
            "virtualserver_flag_password"
        }) {
            assertFalse(ProtocolRedactor.isSecret(key), key);
        }
    }

    @Test
    void modelAndResponseDiagnosticsRedactWhileAccessorsKeepValues() {
        var params = new FileTransferParameters(Map.of("clientftfid", "1", "ftkey", "K3y", "port", "30033"));
        assertFalse(params.toString().contains("K3y"), params.toString());
        assertTrue(params.toString().contains("ftkey=<redacted>"), params.toString());
        assertEquals("K3y", params.getFileTransferKey());

        var response = DefaultArrayResponse.parse("token=aaa token_type=0|token=bbb token_type=1");
        assertEquals("token=<redacted> token_type=0|token=<redacted> token_type=1", response.toString());
        assertEquals("token=aaa token_type=0|token=bbb token_type=1", response.getRawResponse());
        assertEquals("bbb", response.getResponses().get(1).require("token"));
    }
}
