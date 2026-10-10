package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownOutcomeException;
import com.github.theholywaffle.teamspeak3.api.reconnect.CommandRetryPolicy;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectingConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ReconnectSemanticsTest {
	private static final SessionConfiguration SESSION = SessionConfiguration.forServer(7).withNickname("reconnect-bot")
		.withSubscription(TS3EventType.SERVER, -1).withSubscription(TS3EventType.CHANNEL, 12);

	private TS3Config config(int port) {
		return new TS3Config().setHost("127.0.0.1").setQueryPort(port).setFloodRate(TS3Query.FloodRate.UNLIMITED)
			.setLoginCredentials("test", "test").setSessionConfiguration(SESSION)
			.setReconnectStrategy(ReconnectStrategy.exponentialBackoff(10, 2, 40).withMaxAttempts(3))
			.setConnectTimeout(Duration.ofMillis(300)).setHandshakeTimeout(Duration.ofMillis(500))
			.setCommandResponseTimeout(Duration.ofMillis(500)).setQueueWaitTimeout(Duration.ofSeconds(3))
			.setCloseTimeout(Duration.ofMillis(500));
	}

	private void closed(TS3Query query) throws Exception {
		long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		while ((!query.resourcesTerminated() || query.getState() != TS3Query.State.CLOSED) && System.nanoTime() < until) Thread.sleep(5);
		assertEquals(TS3Query.State.CLOSED, query.getState());
		assertTrue(query.resourcesTerminated());
	}

	@Test
	void lostChannelCreateResponseNeverCreatesDuplicateAndUnsentCommandsKeepTheirOrder() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("channelcreate channel_name=first"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.restore(); p.expect("channelcreate channel_name=second"); p.reply("cid=22");
				p.expect("channelcreate channel_name=third"); p.reply("cid=23"); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()))) {
				query.connect();
				var first = query.getAsyncApi().createChannel("first", Map.of());
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var second = query.getAsyncApi().createChannel("second", Map.of());
				var third = query.getAsyncApi().createChannel("third", Map.of());
				drop.countDown();
				var unknown = assertThrows(TS3UnknownOutcomeException.class, () -> first.get(2, TimeUnit.SECONDS));
				assertEquals("channelcreate", unknown.getCommandName());
				assertEquals(22, second.get(2, TimeUnit.SECONDS));
				assertEquals(23, third.get(2, TimeUnit.SECONDS));
				query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void optedInReadReplaysBeforeUnsentReadAndDiscardsPartialResponse() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); p.write("client_id=999\n"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.restore(); p.expect("whoami"); p.reply("client_id=42 virtualserver_id=7");
				p.expect("version"); p.reply("version=test build=1 platform=test"); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()).setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().getVersion();
				drop.countDown();
				assertEquals(42, read.get(2, TimeUnit.SECONDS).getId());
				assertEquals(7, read.get().getVirtualServerId());
				assertEquals("test", waiting.get(2, TimeUnit.SECONDS).getVersion());
				query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void retryExhaustionFailsReadAndStillReleasesUnsentCommands() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.restore(); p.expect("whoami"); },
			p -> { p.restore(); p.expect("version"); p.reply("version=after-exhaustion"); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()).setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().getVersion();
				drop.countDown();
				assertThrows(TS3UnknownOutcomeException.class, () -> read.get(2, TimeUnit.SECONDS));
				assertEquals("after-exhaustion", waiting.get(2, TimeUnit.SECONDS).getVersion());
				query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void defaultPolicyDoesNotReplayEvenReads() throws Exception {
		try (var server = new Peer(p -> { p.restore(); p.expect("whoami"); }, p -> { p.restore(); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()))) {
				query.connect();
				assertThrows(TS3UnknownOutcomeException.class, () -> query.getAsyncApi().whoAmI().get(2, TimeUnit.SECONDS));
				assertTrue(server.secondSession.await(2, TimeUnit.SECONDS));
				awaitConnected(query);
				query.exit(); server.await(); closed(query);
			}
		}
	}

	private void awaitConnected(TS3Query query) throws Exception {
		long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		while (!query.isConnected() && System.nanoTime() < until) Thread.sleep(5);
		assertTrue(query.isConnected());
	}

	@Test
	void cancellationDuringBackoffPreventsReadReplayAndQueuedWrite() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.restore(); p.expect("version"); p.reply("version=next"); p.quit(); })) {
			try (var query = new TS3Query(config(server.port())
				.setReconnectStrategy(ReconnectStrategy.constantBackoff(300).withMaxAttempts(2))
				.setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var write = query.getAsyncApi().createChannel("cancelled", Map.of());
				var next = query.getAsyncApi().getVersion();
				drop.countDown();
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
				while (query.getState() != TS3Query.State.DISCONNECTED && System.nanoTime() < until) Thread.sleep(2);
				assertEquals(TS3Query.State.DISCONNECTED, query.getState());
				assertTrue(read.cancel(true)); assertTrue(write.cancel(true));
				assertEquals("next", next.get(2, TimeUnit.SECONDS).getVersion());
				query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void restorationFailureExhaustsAttemptsWithoutSendingApplicationCommands() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		Script failed = p -> { p.login(); p.expect("use sid=7 -virtual"); p.write("error id=2816 msg=invalid_server_id\n"); };
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); }, failed, failed, failed)) {
			try (var query = new TS3Query(config(server.port()).setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().createChannel("never-sent", Map.of());
				drop.countDown();
				assertThrows(TS3Exception.class, () -> waiting.get(3, TimeUnit.SECONDS));
				assertThrows(TS3Exception.class, () -> read.get(1, TimeUnit.SECONDS));
				closed(query); server.await();
			}
		}
	}

	@Test
	void closingDuringBackoffCancelsReconnectAndSettlesRetainedCommands() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); })) {
			try (var query = new TS3Query(config(server.port())
				.setReconnectStrategy(ReconnectStrategy.constantBackoff(10000))
				.setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().getVersion();
				drop.countDown();
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
				while (query.getState() != TS3Query.State.DISCONNECTED && System.nanoTime() < until) Thread.sleep(2);
				assertEquals(TS3Query.State.DISCONNECTED, query.getState());
				assertTimeout(Duration.ofMillis(700), query::close);
				assertThrows(TS3Exception.class, () -> read.get(1, TimeUnit.SECONDS));
				assertThrows(TS3Exception.class, () -> waiting.get(1, TimeUnit.SECONDS));
				closed(query); server.await();
			}
		}
	}

	@Test
	void serverErrorsDoNotRetryAndSessionChangesAreRejected() throws Exception {
		try (var server = new Peer(p -> { p.restore(); p.expect("whoami"); p.write("error id=256 msg=error\n");
			p.expect("version"); p.reply("version=framing-ok"); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()).setCommandRetryPolicy(CommandRetryPolicy.safeReads(2, "whoami")))) {
				query.connect();
				assertThrows(TS3CommandFailedException.class, () -> query.getAsyncApi().whoAmI().get(1, TimeUnit.SECONDS));
				assertThrows(TS3Exception.class, () -> query.getApi().selectVirtualServerById(8));
				assertThrows(TS3Exception.class, () -> query.getApi().setNickname("other"));
				assertEquals("framing-ok", query.getApi().getVersion().getVersion());
				query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void unsentQueueDeadlineSurvivesReconnectBackoff() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.restore(); p.quit(); })) {
			try (var query = new TS3Query(config(server.port()).setQueueWaitTimeout(Duration.ofMillis(100))
				.setReconnectStrategy(ReconnectStrategy.constantBackoff(400)))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().createChannel("expired", Map.of());
				drop.countDown();
				assertThrows(TS3UnknownOutcomeException.class, () -> read.get(1, TimeUnit.SECONDS));
				var failure = assertThrows(TS3Exception.class, () -> waiting.get(1, TimeUnit.SECONDS));
				assertTrue(failure.getMessage().contains("queue wait"));
				assertTrue(server.secondSession.await(2, TimeUnit.SECONDS));
				awaitConnected(query); query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void interruptedReconnectInitializationStopsWithoutSendingRetainedWork() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		var login = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.restore(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.expect("login test test"); login.countDown(); assertNull(p.in.readLine()); })) {
			try (var query = new TS3Query(config(server.port())
				.setHandshakeTimeout(Duration.ofSeconds(3)).setCommandResponseTimeout(Duration.ofSeconds(3))
				.setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami")))) {
				query.connect();
				var read = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var waiting = query.getAsyncApi().createChannel("never-sent", Map.of());
				drop.countDown();
				assertTrue(login.await(2, TimeUnit.SECONDS));
				assertTimeout(Duration.ofMillis(700), query::close);
				assertThrows(TS3Exception.class, () -> read.get(1, TimeUnit.SECONDS));
				assertThrows(TS3Exception.class, () -> waiting.get(1, TimeUnit.SECONDS));
				closed(query); server.await();
			}
		}
	}

	@Test
	void absentSessionFailsUnsentWorkRatherThanChangingItsTarget() throws Exception {
		var received = new CountDownLatch(1);
		var drop = new CountDownLatch(1);
		try (var server = new Peer(
			p -> { p.login(); p.expect("whoami"); received.countDown(); assertTrue(drop.await(2, TimeUnit.SECONDS)); },
			p -> { p.login(); p.quit(); })) {
			var config = new TS3Config().setHost("127.0.0.1").setQueryPort(server.port()).setLoginCredentials("test", "test")
				.setFloodRate(TS3Query.FloodRate.UNLIMITED).setReconnectStrategy(ReconnectStrategy.constantBackoff(300));
			try (var query = new TS3Query(config)) {
				query.connect();
				var sent = query.getAsyncApi().whoAmI();
				assertTrue(received.await(1, TimeUnit.SECONDS));
				var unsent = query.getAsyncApi().createChannel("never-sent", Map.of());
				drop.countDown();
				assertThrows(TS3Exception.class, () -> unsent.get(1, TimeUnit.SECONDS));
				assertThrows(TS3UnknownOutcomeException.class, () -> sent.get(1, TimeUnit.SECONDS));
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
				while (query.getState() != TS3Query.State.DISCONNECTED && System.nanoTime() < until) Thread.sleep(2);
				assertThrows(TS3Exception.class, () -> query.getAsyncApi().createChannel("during-backoff", Map.of()).get());
				awaitConnected(query); query.exit(); server.await(); closed(query);
			}
		}
	}

	@Test
	void staleReaderCannotAcknowledgeACommandOnItsReplacementConnection() throws Exception {
		var config = config(1).setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami"));
		try (var query = new TS3Query(config)) {
			var queue = CommandQueue.newGlobalQueue(query, false);
			var oldConnection = new Connection(query, config, queue);
			var newConnection = new Connection(query, config, queue);
			var future = queue.getAsyncApi().whoAmI();
			var command = queue.transferCommand(oldConnection);
			queue.prepareReconnect(); queue.resume();
			assertSame(command, queue.transferCommand(newConnection));
			var stale = new CountDownLatch(1);
			queue.completeResponse(command, oldConnection, stale::countDown);
			assertEquals(1, stale.getCount()); assertFalse(future.isDone());
			assertSame(command, queue.peekReceiveQueue());
			var accepted = new CountDownLatch(1);
			queue.completeResponse(command, newConnection, accepted::countDown);
			assertTrue(accepted.await(1, TimeUnit.SECONDS)); assertTrue(queue.isEmpty());
			oldConnection.disconnect(); newConnection.disconnect();
		}
	}

	@Test
	void interruptDuringReconnectSleepPreservesInterruptAndStopsAttempts() throws Exception {
		int port;
		try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { port = listener.getLocalPort(); }
		try (var query = new TS3Query(config(port))) {
			assertThrows(TS3ConnectionFailedException.class, query::connect);
			assertEquals(TS3Query.State.DISCONNECTED, query.getState());
			var entered = new CountDownLatch(1);
			var interrupted = new CompletableFuture<Boolean>();
			var handler = new ReconnectingConnectionHandler(new ConnectionHandler() {
				@Override public void onConnect(TS3Api api) { fail("Interrupted sleep must not connect"); }
				@Override public void onDisconnect(TS3Query query) { entered.countDown(); }
			}, 10_000, 10_000, 0, 1, 2);
			Thread worker = Thread.ofVirtual().start(() -> {
				handler.onDisconnect(query); interrupted.complete(Thread.currentThread().isInterrupted());
			});
			try {
				assertTrue(entered.await(1, TimeUnit.SECONDS));
				worker.interrupt(); assertTrue(interrupted.get(1, TimeUnit.SECONDS));
				assertEquals(TS3Query.State.DISCONNECTED, query.getState());
			} finally { worker.interrupt(); worker.join(1000); }
			assertFalse(worker.isAlive()); query.close(); closed(query);
		}
	}

	@Test
	void retryAndSessionInputsAreValidatedAndFrozen() {
		assertThrows(IllegalArgumentException.class, () -> CommandRetryPolicy.safeReads(1, "channelcreate"));
		assertThrows(IllegalArgumentException.class, () -> CommandRetryPolicy.safeReads(0, "whoami"));
		assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.exponentialBackoff(10, Double.NaN, 20));
		assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.linearBackoff(20, 2, 10));
		assertThrows(IllegalArgumentException.class, () -> ReconnectStrategy.constantBackoff(10).withMaxAttempts(0));
		assertThrows(IllegalArgumentException.class, () -> SessionConfiguration.forServer(0));
		assertThrows(IllegalStateException.class, () -> new TS3Query(new TS3Config().setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami"))));
		var config = config(1);
		try (var query = new TS3Query(config)) {
			assertThrows(IllegalStateException.class, () -> config.setSessionConfiguration(SESSION));
			assertThrows(IllegalStateException.class, () -> config.setCommandRetryPolicy(CommandRetryPolicy.none()));
		}
	}

	@FunctionalInterface private interface Script { void run(Peer peer) throws Exception; }

	/** Multiple scripted connections on one endpoint, with observable assertion failures. */
	private static final class Peer implements AutoCloseable {
		private final ServerSocket listener;
		private final CompletableFuture<Void> finished = new CompletableFuture<>();
		private final CountDownLatch secondSession = new CountDownLatch(1);
		private final Thread worker;
		private volatile Socket socket;
		private BufferedReader in;
		private OutputStream out;
		private int sessions;

		Peer(Script... scripts) throws Exception {
			listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()); listener.setSoTimeout(5000);
			worker = Thread.ofVirtual().start(() -> {
				try {
					for (Script script : scripts) {
						try (Socket accepted = listener.accept()) {
							socket = accepted; accepted.setSoTimeout(3000);
							in = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
							out = accepted.getOutputStream(); script.run(this);
						}
					}
					finished.complete(null);
				} catch (Throwable failure) { finished.completeExceptionally(failure); }
			});
		}
		int port() { return listener.getLocalPort(); }
		void expect(String command) throws Exception { assertEquals(command, in.readLine()); }
		void write(String text) throws Exception { out.write(text.getBytes(StandardCharsets.UTF_8)); out.flush(); }
		void reply(String response) throws Exception { write(response + "\nerror id=0 msg=ok\n"); }
		void login() throws Exception { expect("login test test"); reply(""); }
		void restore() throws Exception {
			login(); expect("use sid=7 -virtual"); reply("");
			expect("clientupdate client_nickname=reconnect-bot"); reply("");
			expect("servernotifyregister event=server"); reply("");
			expect("servernotifyregister event=channel id=12"); reply(""); if (++sessions == 2) secondSession.countDown();
		}
		void quit() throws Exception { expect("quit"); reply(""); }
		void await() throws Exception { finished.get(5, TimeUnit.SECONDS); }
		@Override public void close() throws Exception {
			listener.close(); if (socket != null) socket.close(); worker.join(5000);
			assertFalse(worker.isAlive(), "Scripted peer must stop");
			finished.get(1, TimeUnit.SECONDS);
		}
	}
}
