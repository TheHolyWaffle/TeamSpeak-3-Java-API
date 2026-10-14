package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class CommonWorkflowTest {
    private static final byte[] RULES = "Be nice".getBytes(StandardCharsets.UTF_8);
    private static final String WELCOME = "Welcome\\sto\\sthe\\slobby";
    private static final String CHANNEL_LIST = "channellist -topic -flags -voice -limits -icon -secondsempty -banners";

    @Test
    void documentedWorkflowRunsInBothForms() throws Exception {
        List<String> uploads = new CopyOnWriteArrayList<>();
        try (var files = new ServerSocket(0, 2, InetAddress.getLoopbackAddress());
                var peer = new FakeServerQuery(p -> {
                    p.expect(CHANNEL_LIST);
                    p.write("cid=1 pid=0 channel_order=0 channel_name=Default\n" + FakeServerQuery.OK);
                    p.expect("channelcreate channel_name=Lobby channel_flag_permanent=1");
                    p.write("cid=5\n" + FakeServerQuery.OK);
                    p.expect("channeladdperm cid=5 permsid=i_channel_needed_join_power permvalue=10");
                    p.write(FakeServerQuery.OK);
                    p.expect(
                            "ftinitupload clientftfid=0 name=\\/rules.txt cid=5 cpw= size=7 overwrite=1 resume=0 proto=0");
                    p.write("clientftfid=0 serverftfid=1 ftkey=k port=" + files.getLocalPort() + " seekpos=0\n"
                            + FakeServerQuery.OK);
                    p.expect("clientgetids cluid=online");
                    p.write("cluid=online clid=7 name=Alice\n" + FakeServerQuery.OK);
                    p.expect("clientinfo clid=7");
                    p.write("client_nickname=Alice\n" + FakeServerQuery.OK);
                    p.expect("clientmove clid=7 cid=5");
                    p.write(FakeServerQuery.OK);
                    p.expect("sendtextmessage targetmode=1 target=7 msg=" + WELCOME);
                    p.write(FakeServerQuery.OK);

                    p.expect(CHANNEL_LIST);
                    p.write(
                            "cid=1 pid=0 channel_order=0 channel_name=Default|cid=5 pid=0 channel_order=1 channel_name=Lobby\n"
                                    + FakeServerQuery.OK);
                    p.expect("channeladdperm cid=5 permsid=i_channel_needed_join_power permvalue=10");
                    p.write(FakeServerQuery.OK);
                    p.expect(
                            "ftinitupload clientftfid=1 name=\\/rules.txt cid=5 cpw= size=7 overwrite=1 resume=0 proto=0");
                    p.write("clientftfid=1 serverftfid=2 ftkey=k port=" + files.getLocalPort() + " seekpos=0\n"
                            + FakeServerQuery.OK);
                    p.expect("clientgetids cluid=offline");
                    p.write(FakeServerQuery.EMPTY);
                    p.expect("messageadd cluid=offline subject=Lobby message=" + WELCOME);
                    p.write(FakeServerQuery.OK);
                    p.finishQuit();
                });
                var query = new TS3Query(new TS3Config()
                        .setHost("127.0.0.1")
                        .setQueryPort(peer.port())
                        .setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
            files.setSoTimeout(5000);
            var fileServer = Thread.ofVirtual().start(() -> {
                for (int i = 0; i < 2; i++) {
                    try (var socket = files.accept()) {
                        assertEquals('k', socket.getInputStream().read());
                        uploads.add(
                                new String(socket.getInputStream().readNBytes(RULES.length), StandardCharsets.UTF_8));
                    } catch (Exception failure) {
                        throw new AssertionError(failure);
                    }
                }
            });
            query.connect();

            assertEquals(5, CommonWorkflow.prepareLobby(query.getApi(), "online", RULES));
            assertEquals(
                    5,
                    CommonWorkflow.prepareLobby(query.getAsyncApi(), "offline", RULES)
                            .get(5, TimeUnit.SECONDS));

            fileServer.join(5000);
            assertEquals(List.of("Be nice", "Be nice"), uploads);
            query.exit();
            peer.await();
        }
    }
}
