package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class EventResilienceTest {
	private static TS3Event event(int index) { return listener -> listener.onTextMessage(new TextMessageEvent(
		new com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper(java.util.Map.of("msg", "" + index)))); }

	private static void await(CountDownLatch latch) {
		try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
	}

	@Test void overflowDropsNewestAndPreservesOrderingAfterThrowingCallback() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var delivered = new CountDownLatch(3);
		var received = new CopyOnWriteArrayList<Integer>();
		try (var query = new TS3Query(new TS3Config().setListenerQueueCapacity(2))) {
			query.subscribe(new TS3EventAdapter() {
				@Override public void onTextMessage(TextMessageEvent e) {
					int value = Integer.parseInt(e.getMessage());
					received.add(value);
					delivered.countDown();
					if (value == 0) { entered.countDown(); await(release); throw new IllegalStateException("listener failure"); }
				}
			});
			try {
				query.getEventManager().fireEvent(event(0)); await(entered);
				for (int i = 1; i <= 100; i++) query.getEventManager().fireEvent(event(i));
				assertEquals(98, query.getEventStatistics().droppedEvents());
				release.countDown(); await(delivered);
				assertEquals(List.of(0, 1, 2), received);
				assertEquals(1, query.getEventStatistics().listenerFailures());
			} finally { release.countDown(); }
		}
	}

	@Test void removingSubscriptionDiscardsWaitingEventsAndDoesNotWaitForCallback() {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var calls = new AtomicInteger();
		try (var query = new TS3Query(new TS3Config().setListenerCapacity(1))) {
			var listener = new TS3EventAdapter() {
				@Override public void onTextMessage(TextMessageEvent e) { calls.incrementAndGet(); entered.countDown(); await(release); }
			};
			var subscription = query.subscribe(listener);
			try {
				assertThrows(IllegalStateException.class, () -> query.subscribe(listener));
				query.getEventManager().fireEvent(event(0)); await(entered);
				query.getEventManager().fireEvent(event(1));
				subscription.close(); subscription.close();
				query.getEventManager().fireEvent(event(2));
				release.countDown();
				try (var next = query.subscribe(listener)) { }
				assertEquals(1, calls.get());
			} finally { release.countDown(); }
			query.close();
			assertThrows(IllegalStateException.class, () -> query.subscribe(listener));
		}
	}

	@Test void removingQueuedSubscriptionFreesExecutorSlot() {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var resumed = new CountDownLatch(1);
		try (var query = new TS3Query(new TS3Config().setEventCallbackThreads(1).setListenerCapacity(1))) {
			var blocked = query.subscribe(new TS3EventAdapter() {
				@Override public void onTextMessage(TextMessageEvent e) { entered.countDown(); await(release); }
			});
			try {
				query.getEventManager().fireEvent(event(0)); await(entered); blocked.close();
				var listener = new TS3EventAdapter() { @Override public void onTextMessage(TextMessageEvent e) { resumed.countDown(); } };
				query.subscribe(listener); query.getEventManager().fireEvent(event(1));
				// Removing the queued registration frees its executor task slot.
				query.getApi().removeTS3Listeners(listener);
				query.subscribe(listener); query.getEventManager().fireEvent(event(2));
				assertEquals(0, query.getEventStatistics().rejectedTasks());
				release.countDown(); await(resumed);
			} finally { release.countDown(); }
		}
	}

	@Test void slowListenerUnknownAndMalformedNotificationsDoNotStopCommands() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var duplicateMoves = new CountDownLatch(3);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami");
			p.write("notifyfutureevent extra=1\nnotifytextmessage\nnotifytextmessage msg=first\nnotifyclientmoved ctid=invalid\n");
			for (int i = 0; i < 3; i++) p.write("notifyclientmoved clid=2 ctid=3 reasonid=1\n");
			p.write("client_id=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setFloodRate(TS3Query.FloodRate.UNLIMITED).setConnectionHandler(new ConnectionHandler() {
				@Override public void onConnect(TS3Api api) { api.whoAmI(); }
				@Override public void onDisconnect(TS3Query query) { }
			}))) {
			query.subscribe(new TS3EventAdapter() { @Override public void onTextMessage(TextMessageEvent e) { entered.countDown(); await(release); } });
			query.subscribe(new TS3EventAdapter() { @Override public void onClientMoved(ClientMovedEvent e) { assertFalse(Thread.currentThread().getName().contains("StreamReader")); e.getTargetChannelId(); duplicateMoves.countDown(); } });
			try {
				query.connect(); await(entered); await(duplicateMoves);
				assertEquals(2, query.getAsyncApi().whoAmI().get(3, TimeUnit.SECONDS).getId());
				assertEquals(1, query.getEventStatistics().unknownNotifications());
				assertEquals(1, query.getEventStatistics().malformedNotifications());
				assertEquals(1, query.getEventStatistics().listenerFailures());
			} finally { release.countDown(); query.exit(); }
			peer.await();
		}
	}

	@Test void commandAdmissionIncludesBlockedCompletionCallbacks() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var response = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); await(response); p.write("client_id=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setFloodRate(TS3Query.FloodRate.UNLIMITED).setCommandCapacity(1))) {
			query.connect();
			var first = query.getAsyncApi().whoAmI();
			first.thenAccept(value -> { entered.countDown(); await(release); });
			try {
				assertThrows(TS3QueueFullException.class, () -> FutureAssertions.read(query.getAsyncApi().whoAmI()));
				response.countDown(); await(entered);
				assertThrows(TS3QueueFullException.class, () -> FutureAssertions.read(query.getAsyncApi().whoAmI()));
				release.countDown();
				// Wait for completion to release admission; rejected commands never reach the wire.
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
				while (true) {
					try { assertEquals(2, FutureAssertions.read(query.getAsyncApi().whoAmI()).getId()); break; }
					catch (TS3QueueFullException full) { if (System.nanoTime() >= until) throw full; Thread.sleep(1); }
				}
				// get observes the value before completion callbacks return and release the slot.
				while (query.commandAdmission(true).availablePermits() == 0 && System.nanoTime() < until) Thread.sleep(1);
				assertEquals(1, query.commandAdmission(true).availablePermits());
			} finally { response.countDown(); release.countDown(); query.exit(); }
			peer.await();
		}
	}

	@Test void eventCallbackCanCloseItsQueryWithoutWaitingForItself() throws Exception {
		var closed = new CompletableFuture<Void>();
		var query = new TS3Query(new TS3Config().setCloseTimeout(java.time.Duration.ofMillis(500)));
		query.subscribe(new TS3EventAdapter() {
			@Override public void onTextMessage(TextMessageEvent e) { query.close(); closed.complete(null); }
		});
		try {
			query.getEventManager().fireEvent(event(1)); closed.get(2, TimeUnit.SECONDS);
			assertEquals(TS3Query.State.CLOSED, query.getState());
		} finally { query.close(); }
	}

	@Test void expiredWaitingCommandsReleaseAdmission() throws Exception {
		try (var query = new TS3Query(new TS3Config().setCommandCapacity(1)
			.setQueueWaitTimeout(java.time.Duration.ofMillis(30)))) {
			var first = query.getAsyncApi().whoAmI();
			assertThrows(TS3QueueFullException.class, () -> FutureAssertions.read(query.getAsyncApi().whoAmI()));
			assertThrows(com.github.theholywaffle.teamspeak3.api.exception.TS3Exception.class,
				() -> FutureAssertions.read(first, 3, TimeUnit.SECONDS));
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
			while (true) {
				var next = query.getAsyncApi().whoAmI();
				if (!next.isCompletedExceptionally()) break;
				if (System.nanoTime() >= until) fail("Expired command must release admission");
				Thread.sleep(1);
			}
		}
	}

	@Test void startupAdmissionIsSharedAcrossConnectionGenerationsAndSeparateFromApplicationCommands() {
		try (var query = new TS3Query(new TS3Config().setCommandCapacity(1))) {
			var oldStartup = CommandQueue.newConnectQueue(query);
			var newStartup = CommandQueue.newConnectQueue(query);
			try {
				var oldCommand = oldStartup.getAsyncApi().whoAmI();
				assertFalse(oldCommand.isDone());
				assertThrows(TS3QueueFullException.class, () -> FutureAssertions.read(newStartup.getAsyncApi().whoAmI()));
				assertFalse(query.getAsyncApi().whoAmI().isDone(), "Startup saturation must leave application capacity independent");
			} finally { oldStartup.failRemainingCommands(); newStartup.failRemainingCommands(); }
		}
	}

	@Test void configurationAndBatchRegistrationValidateBeforeChangingState() {
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setCommandCapacity(0));
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setListenerCapacity(-1));
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setListenerQueueCapacity(0));
		assertThrows(IllegalArgumentException.class, () -> new TS3Config().setEventCallbackThreads(0));
		var config = new TS3Config().setListenerCapacity(2);
		try (var query = new TS3Query(config)) {
			assertThrows(IllegalStateException.class, () -> config.setCommandCapacity(2));
			assertThrows(NullPointerException.class, () -> query.getApi().addTS3Listeners(new TS3EventAdapter() {}, null));
			try (var subscription = query.subscribe(new TS3EventAdapter() {})) { }
		}
	}
}
