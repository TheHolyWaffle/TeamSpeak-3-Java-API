package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ThreadModelTest {

	@Test
	void ownedThreadsHaveExplicitKindAndDaemonStatus() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.write("TS3\n");
			p.expect("whoami");
			p.write("virtualserver_status=online client_id=1\n" + FakeServerQuery.OK);
			p.write("notifytextmessage targetmode=3 msg=hi invokerid=1 invokername=a invokeruid=u\n");
			p.finishQuit();
		})) {
			var config = new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
				.setFloodRate(TS3Query.FloodRate.UNLIMITED).setCloseTimeout(Duration.ofSeconds(2));
			// Inherited daemon status would make every owned platform thread a daemon here.
			var query = CompletableFuture.supplyAsync(() -> new TS3Query(config), Thread.ofVirtual()::start).get();
			try (query) {
				var callbackThread = new CompletableFuture<Thread>();
				query.subscribe(new TS3EventAdapter() {
					@Override public void onTextMessage(TextMessageEvent e) { callbackThread.complete(Thread.currentThread()); }
				});
				var completionThread = new CompletableFuture<Thread>();
				query.connect();
				query.getAsyncApi().whoAmI().thenRun(() -> completionThread.complete(Thread.currentThread()))
					.get(5, TimeUnit.SECONDS);

				Map<String, Thread> platform = Thread.getAllStackTraces().keySet().stream()
					.filter(thread -> thread.getName().startsWith("[TeamSpeak-3-Java-API]"))
					.collect(Collectors.toMap(Thread::getName, thread -> thread, (a, b) -> a));
				assertFalse(platform.get("[TeamSpeak-3-Java-API] Deadlines").isDaemon(), "An open query keeps the JVM alive");
				assertTrue(platform.get("[TeamSpeak-3-Java-API] StreamReader").isDaemon());
				assertTrue(platform.get("[TeamSpeak-3-Java-API] StreamWriter").isDaemon());
				assertFalse(platform.containsKey("[TeamSpeak-3-Java-API] KeepAlive"), "Keep-alive runs on a virtual thread");

				assertTrue(completionThread.get(5, TimeUnit.SECONDS).isVirtual(), "Command completions run on virtual threads");
				Thread eventWorker = callbackThread.get(5, TimeUnit.SECONDS);
				assertFalse(eventWorker.isVirtual(), "Event callbacks use the bounded platform pool");
				assertTrue(eventWorker.isDaemon());
				query.exit();
				peer.await();
			}
		}
	}
}
