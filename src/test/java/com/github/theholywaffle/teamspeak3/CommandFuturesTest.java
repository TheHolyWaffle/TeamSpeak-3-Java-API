package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandFuturesTest {
	private static TS3Query query(FakeServerQuery peer) {
		return new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setFloodRate(TS3Query.FloodRate.UNLIMITED));
	}
	private static void await(CountDownLatch latch) throws InterruptedException {
		assertTrue(latch.await(5, TimeUnit.SECONDS));
	}

	@Test void apiStagesAllowMultipleObserversAndCaptureCallbackFailures() throws Exception {
		var respond = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); await(respond); p.write("client_id=3\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect(); var source = query.getAsyncApi().whoAmI();
			var one = source.thenApply(value -> value.getId() + 1);
			var two = source.thenApply(value -> value.getId() * 2);
			var thrown = new IllegalArgumentException("callback");
			var bad = source.thenApply(value -> { throw thrown; });
			respond.countDown();
			assertEquals(4, one.get(2, TimeUnit.SECONDS)); assertEquals(6, two.get(2, TimeUnit.SECONDS));
			assertSame(thrown, assertThrows(ExecutionException.class, () -> bad.get(2, TimeUnit.SECONDS)).getCause());
			assertEquals(3, source.get().getId()); query.exit(); peer.await();
		} finally { respond.countDown(); }
	}

	@Test void mappingAndCompositionPropagateFailuresAndCancellation() throws Exception {
		var source = new CompletableFuture<Integer>();
		var mapped = CommandFutures.map(source, value -> value + 1);
		assertTrue(mapped.cancel(true)); assertTrue(source.isCancelled());
		var first = new CompletableFuture<Integer>();
		var next = new CompletableFuture<String>();
		var chained = CommandFutures.compose(first, value -> next);
		first.complete(1); chained.cancel(false); assertTrue(next.isCancelled());
		var failure = new TS3Exception("failed");
		var failed = CommandFutures.map(CompletableFuture.<Integer>failedFuture(failure), value -> value + 1);
		assertSame(failure, assertThrows(ExecutionException.class, failed::get).getCause());
		assertSame(failure, assertThrows(TS3Exception.class, () -> CommandFutures.join(failed)));
		var callbackFailure = new IllegalArgumentException("callback failure");
		assertSame(callbackFailure, assertThrows(TS3Exception.class,
			() -> CommandFutures.join(CompletableFuture.failedFuture(callbackFailure))).getCause());
	}

	@Test void compositionCancellationBeforeSuccessDoesNotStartNextRequest() {
		var source = new CompletableFuture<Integer>();
		var invoked = new AtomicBoolean();
		var result = CommandFutures.compose(source, value -> {
			invoked.set(true); return CompletableFuture.completedFuture(value);
		});
		result.cancel(false); source.complete(1); assertFalse(invoked.get());
	}

	@Test void compositionCancellationWhileNextRequestIsCreatedCancelsIt() throws Exception {
		var source = new CompletableFuture<Integer>();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var next = new CompletableFuture<Integer>();
		var result = CommandFutures.compose(source, value -> {
			entered.countDown();
			try { await(release); } catch (InterruptedException e) { throw new AssertionError(e); }
			return next;
		});
		var completing = Thread.ofVirtual().start(() -> source.complete(1));
		try { await(entered); result.cancel(false); } finally { release.countDown(); }
		completing.join(5000); assertFalse(completing.isAlive()); assertTrue(next.isCancelled());
	}

	@Test void combinedResultsKeepInputOrderFailFastAndSupportNull() throws Exception {
		var first = new CompletableFuture<Integer>(); var second = new CompletableFuture<Integer>();
		var all = CommandFutures.all(List.of(first, second));
		second.complete(null); assertFalse(all.isDone()); first.complete(1);
		assertEquals(java.util.Arrays.asList(1, null), all.get());
		var waiting = new CompletableFuture<Integer>(); var failed = new CompletableFuture<Integer>();
		var result = CommandFutures.all(List.of(waiting, failed));
		var failure = new TS3Exception("failure"); failed.completeExceptionally(failure);
		assertSame(failure, assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS)).getCause());
		assertFalse(waiting.isDone());
		var a = new CompletableFuture<Integer>(); var b = new CompletableFuture<Integer>();
		CommandFutures.all(List.of(a, b)).cancel(false); assertTrue(a.isCancelled()); assertTrue(b.isCancelled());
		assertEquals(List.of(), CommandFutures.all(List.<CompletableFuture<Integer>>of()).get());
	}

	@Test void aggregateCancellationReachesEveryInputBeforeApplicationObservers() throws Exception {
		var first = new CompletableFuture<Integer>();
		var second = new CompletableFuture<Integer>();
		var result = CommandFutures.all(List.of(first, second));
		var observed = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		result.whenComplete((value, failure) -> {
			observed.countDown();
			try { await(release); } catch (InterruptedException e) { throw new AssertionError(e); }
		});
		var cancelling = Thread.ofVirtual().start(() -> result.cancel(false));
		try {
			await(observed);
			assertTrue(first.isCancelled());
			assertTrue(second.isCancelled());
		} finally { release.countDown(); }
		cancelling.join(5000);
		assertFalse(cancelling.isAlive());
	}

	@Test void getTimeoutIsObservationalAndStageTimeoutIsTerminal() throws Exception {
		var source = new CompletableFuture<Integer>();
		assertThrows(TimeoutException.class, () -> source.get(1, TimeUnit.MILLISECONDS));
		assertFalse(source.isDone()); source.complete(7); assertEquals(7, source.get());
		var timed = new CompletableFuture<Integer>().orTimeout(1, TimeUnit.MILLISECONDS);
		assertInstanceOf(TimeoutException.class, assertThrows(ExecutionException.class,
			() -> timed.get(1, TimeUnit.SECONDS)).getCause());
		assertFalse(timed.complete(9));
	}

	@Test void interruptibleGetAndSynchronousAdapterHaveExplicitInterruptSemantics() throws Exception {
		var source = new CompletableFuture<Integer>();
		Thread.currentThread().interrupt();
		assertThrows(InterruptedException.class, source::get); assertFalse(Thread.currentThread().isInterrupted());
		var started = new CountDownLatch(1); var interrupted = new CompletableFuture<Boolean>();
		var waiter = Thread.ofVirtual().start(() -> {
			Thread.currentThread().interrupt(); started.countDown();
			assertEquals(3, CommandFutures.join(source));
			interrupted.complete(Thread.currentThread().isInterrupted());
		});
		await(started); source.complete(3);
		assertTrue(interrupted.get(1, TimeUnit.SECONDS)); waiter.join(1000);
	}

	@Test void unsentCancellationImmediatelyReleasesAdmissionAndNeverReachesServer() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setCommandCapacity(1))) {
			var cancelled = query.getAsyncApi().createChannel("must-not-send", java.util.Map.of());
			assertTrue(cancelled.cancel(false));
			var next = query.getAsyncApi().whoAmI(); assertFalse(next.isDone());
			query.connect(); assertEquals(2, next.get(2, TimeUnit.SECONDS).getId());
			query.exit(); peer.await();
		}
	}

	@Test void sentCancellationConsumesResponseAndCannotUndoMutation() throws Exception {
		var received = new CountDownLatch(1); var respond = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("channelcreate channel_name=sent"); received.countDown(); await(respond);
			p.write("cid=99\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect();
			var sent = query.getAsyncApi().createChannel("sent", java.util.Map.of());
			await(received); assertTrue(sent.cancel(true));
			var next = query.getAsyncApi().whoAmI(); respond.countDown();
			assertThrows(CancellationException.class, sent::get);
			assertEquals(2, next.get(2, TimeUnit.SECONDS).getId()); query.exit(); peer.await();
		} finally { respond.countDown(); }
	}

	@Test void responseCompletionsAreOrderedAndCallbacksRunAwayFromProtocolThread() throws Exception {
		var respond = new CountDownLatch(1); var completed = new CountDownLatch(2);
		var order = new CopyOnWriteArrayList<Integer>(); var threads = new CopyOnWriteArrayList<String>();
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); await(respond); p.write("client_id=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect();
			var first = query.getAsyncApi().whoAmI(); var second = query.getAsyncApi().whoAmI();
			for (var future : List.of(first, second)) future.thenAccept(value -> {
				threads.add(Thread.currentThread().getName()); order.add(value.getId()); completed.countDown();
			});
			respond.countDown(); await(completed); assertEquals(List.of(1, 2), order);
			assertTrue(threads.stream().allMatch(name -> name.contains("Completions")));
			var caller = Thread.currentThread();
			assertSame(caller, first.thenApply(value -> Thread.currentThread()).get());
			try (var executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("chosen-callback").factory())) {
				assertEquals("chosen-callback", first.thenApplyAsync(value -> Thread.currentThread().getName(), executor).get());
			}
			query.exit(); peer.await();
		} finally { respond.countDown(); }
	}

	@Test void commandFailureUsesJdkWrappersAndSynchronousApiTranslatesThem() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("error id=512 msg=invalid_client\n");
			p.expect("whoami"); p.write("error id=512 msg=invalid_client\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect();
			var failure = assertThrows(ExecutionException.class, () -> query.getAsyncApi().whoAmI().get(2, TimeUnit.SECONDS));
			assertEquals(512, assertInstanceOf(TS3CommandFailedException.class, failure.getCause()).getError().getId());
			assertThrows(TS3CommandFailedException.class, () -> query.getApi().whoAmI());
			query.exit(); peer.await();
		}
	}
	@Test void apiStageTimeoutStillConsumesLateResponse() throws Exception {
		var received = new CountDownLatch(1); var respond = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); received.countDown(); await(respond);
			p.write("client_id=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect(); var first = query.getAsyncApi().whoAmI(); await(received);
			first.orTimeout(1, TimeUnit.MILLISECONDS);
			assertInstanceOf(TimeoutException.class, assertThrows(ExecutionException.class,
				() -> first.get(2, TimeUnit.SECONDS)).getCause());
			var next = query.getAsyncApi().whoAmI(); respond.countDown();
			assertEquals(2, next.get(2, TimeUnit.SECONDS).getId()); query.exit(); peer.await();
		} finally { respond.countDown(); }
	}

	@Test void expectedServerErrorRecoveryWorksThroughJdkWrappers() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("clientinfo clid=7"); p.write("error id=512 msg=invalid_client\n");
			p.expect("clientinfo clid=8"); p.write("error id=2568 msg=insufficient_permissions\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect(); assertFalse(query.getAsyncApi().isClientOnline(7).get(2, TimeUnit.SECONDS));
			var failure = assertThrows(ExecutionException.class,
				() -> query.getAsyncApi().isClientOnline(8).get(2, TimeUnit.SECONDS));
			assertEquals(2568, assertInstanceOf(TS3CommandFailedException.class, failure.getCause()).getError().getId());
			query.exit(); peer.await();
		}
	}

	@Test void closeDoesNotAbandonCompletionsBehindABlockedCallback() throws Exception {
		var respond = new CountDownLatch(1); var entered = new CountDownLatch(1);
		var secondResponse = new CountDownLatch(1); var release = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); await(respond); p.write("client_id=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); secondResponse.countDown();
			p.expect(null);
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port())
			.setFloodRate(TS3Query.FloodRate.UNLIMITED).setCloseTimeout(Duration.ofMillis(300)))) {
			query.connect(); var first = query.getAsyncApi().whoAmI(); var second = query.getAsyncApi().whoAmI();
			first.thenAccept(value -> {
				entered.countDown();
				while (release.getCount() != 0) {
					try { release.await(); } catch (InterruptedException ignored) { }
				}
			});
			try {
				respond.countDown(); await(entered); await(secondResponse);
				assertTimeout(Duration.ofMillis(700), query::close);
				// The response can have been accepted or be terminated by close, but must settle.
				assertTrue(second.handle((value, failure) -> true).get(2, TimeUnit.SECONDS));
			} finally { release.countDown(); }
			peer.await();
		} finally { respond.countDown(); release.countDown(); }
	}

	@Test void inlineCompletionCallbackCanCloseItsQuery() throws Exception {
		var respond = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); await(respond); p.write("client_id=1\nerror id=0 msg=ok\n"); p.expect(null);
		}); var query = query(peer)) {
			query.connect(); var first = query.getAsyncApi().whoAmI();
			var closed = first.thenRun(query::close); respond.countDown();
			closed.get(2, TimeUnit.SECONDS); assertEquals(TS3Query.State.CLOSED, query.getState()); peer.await();
		} finally { respond.countDown(); }
	}

	@Test void cancellationReachesUnsentRequestBeforeBlockingApplicationObservers() throws Exception {
		var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setCommandCapacity(1))) {
			var mutation = query.getAsyncApi().createChannel("must-not-send", java.util.Map.of());
			mutation.whenComplete((value, failure) -> {
				entered.countDown();
				try { await(release); } catch (InterruptedException e) { throw new AssertionError(e); }
			});
			var cancelled = new CompletableFuture<Boolean>();
			var cancelling = Thread.ofVirtual().start(() -> cancelled.complete(mutation.cancel(false)));
			try {
				await(entered); assertTrue(mutation.isCancelled());
				var next = query.getAsyncApi().whoAmI(); assertFalse(next.isDone());
				query.connect(); assertEquals(2, next.get(2, TimeUnit.SECONDS).getId());
			} finally { release.countDown(); cancelling.join(5000); }
			assertTrue(cancelled.get(1, TimeUnit.SECONDS)); query.exit(); peer.await();
		} finally { release.countDown(); }
	}

	@Test void lookupErrorRecoversBeforeAnUnrelatedListingResponds() throws Exception {
		var release = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("channelfind pattern=missing"); p.write("error id=768 msg=invalid_channel\n");
			p.expect(com.github.theholywaffle.teamspeak3.commands.ChannelCommands.channelList().toString());
			await(release); p.write("error id=0 msg=ok\n"); p.finishQuit();
		}); var query = query(peer)) {
			query.connect();
			try { assertEquals(List.of(), query.getAsyncApi().getChannelsByName("missing").get(500, TimeUnit.MILLISECONDS)); }
			finally { release.countDown(); }
			query.exit(); peer.await();
		} finally { release.countDown(); }
	}

	@Test void synchronousAdapterRethrowsWrappedCancellation() {
		var source = new CompletableFuture<Integer>(); source.cancel(false);
		var dependent = source.thenApply(value -> value + 1);
		assertThrows(CancellationException.class, () -> CommandFutures.join(dependent));
	}

	@Test void fileWorkRetainsAdmissionEvenWhenItsResultIsCancelled() throws Exception {
		var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
		var fileDone = new CompletableFuture<Void>();
		try (var files = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
		     var peer = new FakeServerQuery(p -> {
			p.expect(com.github.theholywaffle.teamspeak3.commands.FileCommands.ftInitDownload(0, "/test", 0, "").toString());
			p.write("clientftfid=0 serverftfid=1 ftkey=k port=" + files.getLocalPort() + " size=1\nerror id=0 msg=ok\n");
			p.expect("whoami"); p.write("client_id=2\nerror id=0 msg=ok\n"); p.finishQuit();
		}); var query = new TS3Query(new TS3Config().setHost("127.0.0.1").setQueryPort(peer.port()).setCommandCapacity(1))) {
			files.setSoTimeout(5000);
			var fileWorker = Thread.ofVirtual().start(() -> {
				try (var socket = files.accept()) {
					socket.setSoTimeout(5000); assertEquals('k', socket.getInputStream().read());
					socket.getOutputStream().write(7); fileDone.complete(null);
				} catch (Throwable failure) { fileDone.completeExceptionally(failure); }
			});
			var output = new java.io.OutputStream() {
				@Override public void write(int value) throws java.io.IOException {
					entered.countDown();
					try { await(release); } catch (InterruptedException e) { throw new java.io.IOException(e); }
				}
			};
			query.connect(); var transfer = query.getAsyncApi().downloadFile(output, "/test", 0);
			try {
				await(entered); assertTrue(transfer.cancel(false));
				assertThrows(com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException.class,
					() -> FutureAssertions.read(query.getAsyncApi().whoAmI()));
				release.countDown();
				long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
				while (query.commandAdmission(true).availablePermits() == 0 && System.nanoTime() < until) Thread.sleep(1);
				assertEquals(1, query.commandAdmission(true).availablePermits());
				assertEquals(2, query.getAsyncApi().whoAmI().get(2, TimeUnit.SECONDS).getId());
				query.exit(); peer.await(); fileDone.get(2, TimeUnit.SECONDS);
			} finally { release.countDown(); fileWorker.join(5000); }
		} finally { release.countDown(); }
	}

}
