package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class OfflineMessageTest {
    @Test void absentAndEmptyBodiesRemainDistinctInBothApis() throws Exception {
        try (var peer = new FakeServerQuery(p -> {
            p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
            p.expect("messageget msgid=1"); p.write("error id=1281 msg=database\\sempty\\sresult\\sset\n");
            p.expect("messageget msgid=2"); p.write("message=\nerror id=0 msg=ok\n");
            p.expect("messageget msgid=3"); p.write("message=hello\\sworld\\p\\\\s\nerror id=0 msg=ok\n");
            p.finishQuit();
        }); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
                .setFloodRate(TS3Query.FloodRate.UNLIMITED).setConnectionHandler(new ConnectionHandler() {
                    @Override public void onConnect(TS3Api api) { api.whoAmI(); }
                    @Override public void onDisconnect(TS3Query ignored) { }
                }))) {
            query.connect();
            assertNull(query.getApi().getOfflineMessage(1));
            assertEquals("", query.getAsyncApi().getOfflineMessage(2).get(5, TimeUnit.SECONDS));
            assertEquals("hello world|\\s", query.getApi().getOfflineMessage(3));
            query.exit(); peer.await();
        }
    }
}
