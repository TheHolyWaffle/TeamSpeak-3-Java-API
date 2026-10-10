package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ServerQueryProtocolTest {
	private static final String FILE_LIST = "ftgetfilelist cid=7 cpw= path=\\/";

	private TS3Query query(FakeServerQuery peer) {
		return new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setFloodRate(TS3Query.FloodRate.UNLIMITED).setCommandTimeout(2000)
			.setConnectionHandler(new ConnectionHandler() {
				@Override public void onConnect(TS3Api api) { api.whoAmI(); }
				@Override public void onDisconnect(TS3Query query) { }
			}));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "TS3\n", "TS3\n\rWelcome to the TeamSpeak 3 ServerQuery interface.\n\r\n\r", "TS6\r\nWelcome to ServerQuery\r\nextra banner with example=value\r\nanother line\r\n\r\n"})
	void variableBannersDoNotConsumeResponses(String banner) throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			// Startup must send commands without waiting for a fixed banner count.
			p.expect("whoami");
			// Coalesced banner and response reproduce the old ready()-based drain.
			p.write(banner + "clid=1\nerror id=0 msg=ok\n");
			p.expect(FILE_LIST);
			p.write("cid=7 path=\\/ name=first|name=second\nerror id=0 msg=ok\n");
			p.finishQuit();
		})) {
			var query = query(peer);
			try {
				query.connect();
				var rows = query.getAsyncApi().getFileList("/", 7).get(5, TimeUnit.SECONDS);
				assertEquals(2, rows.size());
				assertEquals("/first", rows.get(0).getPath());
				assertEquals("/second", rows.get(1).getPath());
				assertEquals(7, rows.get(1).getChannelId());
			} finally { query.exit(); }
			peer.await();
		}
	}

	@Test
	void fragmentedLinesAndNotificationsWaitForDelayedTerminator() throws Exception {
		var fragmentSent = new CountDownLatch(1);
		var releaseFragment = new CountDownLatch(1);
		var responseStarted = new CountDownLatch(1);
		var releaseResponse = new CountDownLatch(1);
		var notification = new CompletableFuture<String>();
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami");
			p.write("TS3\nclid=1\nerror id=0 msg=ok\n");
			p.expect(FILE_LIST);
			// Split UTF-8 within a multibyte character, and the escape across writes.
			byte[] data = "cid=7 path=\\/ name=日本語\\\\n\r\n".getBytes(StandardCharsets.UTF_8);
			int split = "cid=7 path=\\/ name=".getBytes(StandardCharsets.UTF_8).length + 1;
			p.write(java.util.Arrays.copyOfRange(data, 0, split));
			fragmentSent.countDown();
			assertTrue(releaseFragment.await(5, TimeUnit.SECONDS));
			for (int i = split; i < data.length; i++) p.write(new byte[] {data[i]});
			p.write("notifytextmessage targetmode=1 msg=hello\\sworld invokerid=2 invokername=peer invokeruid=uid\n");
			p.write("notifytextmessage\n"); // malformed notification must not kill the reader
			p.write("name=second\n");
			responseStarted.countDown();
			assertTrue(releaseResponse.await(5, TimeUnit.SECONDS));
			p.write("error id=0 msg=ok\n");
			p.expect(FILE_LIST);
			p.write("name=next\nerror id=0 msg=ok\n");
			p.finishQuit();
		})) {
			var query = query(peer);
			query.getApi().addTS3Listeners(new TS3EventAdapter() {
				@Override public void onTextMessage(TextMessageEvent event) { notification.complete(event.getMessage()); }
			});
			try {
				query.connect();
				var first = query.getAsyncApi().getFileList("/", 7);
				assertTrue(fragmentSent.await(5, TimeUnit.SECONDS));
				assertFalse(first.isDone(), "A partial UTF-8 line must not complete the command");
				releaseFragment.countDown();
				assertTrue(responseStarted.await(5, TimeUnit.SECONDS));
				assertEquals("hello world", notification.get(5, TimeUnit.SECONDS));
				assertFalse(first.isDone(), "Data and notifications must not finish a response");
				var second = query.getAsyncApi().getFileList("/", 7);
				releaseResponse.countDown();
				var rows = first.get(5, TimeUnit.SECONDS);
				assertEquals("日本語\\n", rows.get(0).getName());
				assertEquals("/second", rows.get(1).getPath());
				assertEquals(7, rows.get(1).getChannelId());
				var next = second.get(5, TimeUnit.SECONDS);
				assertEquals(1, next.size());
				assertEquals("next", next.get(0).getName());
				assertFalse(next.get(0).getMap().containsKey("cid"), "Shared fields must not leak between commands");
			} finally { releaseFragment.countDown(); releaseResponse.countDown(); query.exit(); }
			peer.await();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"error id=256 msg=bad\\scommand\n", "error id=1281 msg=empty\n", "error id=0 msg=ok\n"})
	void commandErrorsAndEmptySuccess(String error) throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
			p.expect(FILE_LIST); p.write(error);
			p.expect(FILE_LIST); p.write("name=after\nerror id=0 msg=ok\n");
			p.finishQuit();
		})) {
			var query = query(peer);
			try {
				query.connect();
				var response = query.getAsyncApi().getFileList("/", 7);
				if (error.contains("256")) {
					var failure = assertThrows(TS3CommandFailedException.class, () -> FutureAssertions.read(response, 5, TimeUnit.SECONDS));
					assertEquals("bad command", failure.getError().getMessage());
				} else { assertTrue(response.get(5, TimeUnit.SECONDS).isEmpty()); }
				assertEquals("after", query.getAsyncApi().getFileList("/", 7).get(5, TimeUnit.SECONDS).get(0).getName());
			} finally { query.exit(); }
			peer.await();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "name=partial", "name=partial\n", "error id=0 msg=ok", "error id=invalid msg=bad\n", "error msg=missing_id\n"})
	void eofOrMalformedTerminatorFailsPendingCommand(String incomplete) throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
			p.expect(FILE_LIST); p.write(incomplete);
			// Script return closes the socket: an incomplete response cannot succeed.
		})) {
			var query = query(peer);
			try {
				query.connect();
				var response = query.getAsyncApi().getFileList("/", 7);
				assertThrows(TS3QueryShutDownException.class, () -> FutureAssertions.read(response, 5, TimeUnit.SECONDS));
			} finally { query.exit(); }
			peer.await();
		}
	}
}
