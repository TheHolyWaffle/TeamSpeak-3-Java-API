package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RawCommandTest {
	private static TS3Config config(FakeServerQuery peer) {
		return new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setConnectionHandler(new ConnectionHandler() {
				@Override public void onConnect(TS3Api api) { api.whoAmI(); }
				@Override public void onDisconnect(TS3Query query) { }
			});
	}

	@Test void rejectsInjectionAndNoncanonicalNames() {
		for (String line : new String[]{"", " version", "VERSION", "version|quit", "version\nquit", "version\rquit", "version\0", "version\t", "version\u0085quit"}) {
			assertThrows(IllegalArgumentException.class, () -> QueryCommands.rawCommand(line), line);
		}
		assertThrows(IllegalArgumentException.class, () -> QueryCommands.rawCommand(null));
		var command = QueryCommands.rawCommand("channeledit cid=4 channel_name=literal\\s\\\\s|cid=5");
		assertEquals("channeledit", command.getName());
		assertEquals("channeledit cid=4 channel_name=literal\\s\\\\s|cid=5", command.toString());
	}

	@Test void preservesUnknownFieldsAndTypedErrorsThenContinuesFraming() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("TS3\nclid=1\nerror id=0 msg=ok\n");
			p.expect("futurecommand value=hello\\sworld");
			p.write("new_property=hello\\sworld\nerror id=0 msg=ok\n");
			p.expect("unsupportedcommand"); p.write("error id=256 msg=command\\snot\\sfound\n");
			p.expect("version"); p.write("version=probe\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(config(peer).setFloodRate(TS3Query.FloodRate.UNLIMITED))) {
			query.connect();
			var response = query.getAsyncApi().executeRawCommand("futurecommand value=hello\\sworld").get(5, TimeUnit.SECONDS);
			assertEquals("hello world", response.getFirstResponse().get("new_property"));
			assertEquals("new_property=hello\\sworld", response.getRawResponse());
			var error = assertThrows(TS3CommandFailedException.class, () -> query.getApi().executeRawCommand("unsupportedcommand"));
			assertEquals(256, error.getError().getId());
			assertEquals("command not found", error.getError().getMessage());
			assertEquals("probe", query.getApi().executeRawCommand("version").getFirstResponse().get("version"));
			query.exit(); peer.await();
		}
	}

	@Test void unsentCancellationReleasesAdmissionBeforeApplicationCallbacks() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
			p.expect("version"); p.write("version=probe\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(config(peer).setCommandCapacity(1))) {
			var cancelled = query.getAsyncApi().executeRawCommand("channeldelete cid=5 force=1");
			assertThrows(TS3QueueFullException.class,
				() -> query.getApi().executeRawCommand("version"));
			var replacement = new AtomicReference<CompletableFuture<DefaultArrayResponse>>();
			cancelled.whenComplete((value, failure) -> replacement.set(query.getAsyncApi().executeRawCommand("version")));
			assertTrue(cancelled.cancel(false));
			var next = replacement.get();
			assertNotNull(next);
			assertFalse(next.isDone(), "Application callback must acquire the capacity released by cancellation");
			query.connect(); assertEquals("probe", next.get(5, TimeUnit.SECONDS).getFirstResponse().get("version"));
			query.exit(); peer.await();
		}
	}

	@Test void rawSessionCommandsCannotBypassReconnectContract() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("use sid=1 -virtual"); p.write("TS3\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("clid=1\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(config(peer)
			.setFloodRate(TS3Query.FloodRate.UNLIMITED).setSessionConfiguration(SessionConfiguration.forServer(1))
			.setReconnectStrategy(ReconnectStrategy.constantBackoff(50)))) {
			query.connect();
			assertThrows(TS3Exception.class, () -> query.getApi().executeRawCommand("use sid=2"));
			query.exit(); peer.await();
		}
	}
}
