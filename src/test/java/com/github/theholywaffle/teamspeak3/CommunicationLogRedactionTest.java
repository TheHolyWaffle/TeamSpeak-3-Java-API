package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Runs in its own Surefire execution, which writes DEBUG output of slf4j-simple to the file named
 * by {@code org.slf4j.simpleLogger.logFile}.
 */
@Tag("debug-logging")
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CommunicationLogRedactionTest {
    private static final List<String> SECRETS =
            List.of("LoginSecret", "ChannelSecret", "TransferKey", "TokenSecret", "EventToken", "StrayToken");

    @Test
    void debugCommunicationLogNeverContainsSecrets() throws Exception {
        Path log = Path.of(System.getProperty("org.slf4j.simpleLogger.logFile"));
        try (var peer = new FakeServerQuery(p -> {
                    p.expect("login serveradmin LoginSecret");
                    p.write(FakeServerQuery.OK);
                    p.expect("clientmove clid=5 cid=2 cpw=ChannelSecret");
                    p.write(FakeServerQuery.OK);
                    p.expect("ftinitupload clientftfid=1 name=/a cid=2 cpw=ChannelSecret size=1 overwrite=1 resume=0");
                    p.write("clientftfid=1 serverftfid=7 ftkey=TransferKey port=30033 seekpos=0\n"
                            + FakeServerQuery.OK);
                    p.expect("privilegekeyadd tokentype=0 tokenid1=6 tokenid2=0");
                    p.write("token=TokenSecret\n" + FakeServerQuery.OK);
                    p.write("notifytokenused clid=5 cldbid=3 cluid=u token=EventToken tokencustomset= token1=6"
                            + " token2=0\n");
                    p.write("token=StrayToken\n");
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED)
                        .setLoginCredentials("serveradmin", "LoginSecret")
                        .setEnableCommunicationsLogging(true))) {
            query.connect();
            var api = query.getApi();
            api.executeRawCommand("clientmove clid=5 cid=2 cpw=ChannelSecret");
            api.executeRawCommand(
                    "ftinitupload clientftfid=1 name=/a cid=2 cpw=ChannelSecret size=1 overwrite=1 resume=0");
            assertEquals(
                    "TokenSecret",
                    api.executeRawCommand("privilegekeyadd tokentype=0 tokenid1=6 tokenid2=0")
                            .getFirstResponse()
                            .orElseThrow()
                            .require("token"));
            while (!Files.readString(log).contains("[UNHANDLED]")) Thread.sleep(10);
            query.exit();
            peer.await();
        }

        String output = Files.readString(log);
        for (String expected : List.of(
                "[login] > login serveradmin <redacted>",
                "[clientmove] > clientmove clid=5 cid=2 cpw=<redacted>",
                "[ftinitupload] < clientftfid=1 serverftfid=7 ftkey=<redacted> port=30033",
                "[privilegekeyadd] < token=<redacted>",
                "[event] < notifytokenused clid=5 cldbid=3 cluid=u token=<redacted>",
                "[UNHANDLED] < token=<redacted>")) {
            assertTrue(output.contains(expected), () -> "Missing " + expected + " in:\n" + output);
        }
        for (String secret : SECRETS) {
            assertFalse(output.contains(secret), () -> secret + " leaked into:\n" + output);
        }
    }
}
