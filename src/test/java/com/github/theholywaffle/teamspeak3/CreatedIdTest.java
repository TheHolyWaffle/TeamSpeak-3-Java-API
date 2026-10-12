package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CreatedIdTest {
    @ParameterizedTest @ValueSource(strings = {"", "sgid=bad", "sgid=-1", "sgid=0"})
    void invalidCreationResponseFailsBeforeAnIdCanReachAnotherCommand(String response) throws Exception {
        try (var peer = new FakeServerQuery(p -> {
            p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
            p.expect("servergroupadd name=probe type=1");
            p.write((response.isEmpty() ? "" : response + "\n") + "error id=0 msg=ok\n");
            p.expect("version"); p.write("version=probe\nerror id=0 msg=ok\n");
            p.finishQuit();
        }); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
                .setFloodRate(TS3Query.FloodRate.UNLIMITED).setConnectionHandler(new ConnectionHandler() {
                    @Override public void onConnect(TS3Api api) { api.whoAmI(); }
                    @Override public void onDisconnect(TS3Query ignored) { }
                }))) {
            query.connect();
            var failure = assertThrows(TS3Exception.class, () -> query.getApi().addServerGroup("probe"));
            assertEquals("servergroupadd response requires a positive sgid", failure.getMessage());
            assertEquals("probe", query.getApi().getVersion().getVersion());
            query.exit(); peer.await();
        }
    }
}
