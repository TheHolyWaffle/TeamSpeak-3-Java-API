package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TimeoutException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3TransportException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ErrorCategoriesTest {
	private static TS3Config config(int port) {
		return new TS3Config().setHost("127.0.0.1").setQueryPort(port)
			.setFloodRate(TS3Query.FloodRate.UNLIMITED)
			.setConnectTimeout(Duration.ofMillis(500)).setHandshakeTimeout(Duration.ofMillis(500))
			.setQueueWaitTimeout(Duration.ofMillis(150)).setCommandResponseTimeout(Duration.ofMillis(200))
			.setCloseTimeout(Duration.ofMillis(500));
	}

	@Test void serverErrorsIdentifyCommandAndAreDefinitelyNotExecuted() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("channelinfo cid=99"); p.write("error id=768 msg=invalid\\schannelID\n");
			p.finishQuit();
		}); var query = new TS3Query(config(peer.port()))) {
			query.connect();
			var failure = assertThrows(TS3CommandFailedException.class, () -> query.getApi().channels().info(99));
			assertEquals(768, failure.getErrorId());
			assertEquals("channelinfo", failure.getCommandName());
			assertTrue(failure.matchesQueryError(768));
			assertFalse(failure.isOutcomeUnknown());
			query.exit(); peer.await();
		}
	}

	@Test void queueWaitTimeoutWasNeverSent() throws Exception {
		var release = new CountDownLatch(1);
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami");
			assertTrue(release.await(2, TimeUnit.SECONDS));
			p.write("clid=1\nerror id=0 msg=ok\n");
			p.finishQuit();
		}); var query = new TS3Query(config(peer.port()).setCommandResponseTimeout(Duration.ofSeconds(2))
			.setReconnectStrategy(ReconnectStrategy.exponentialBackoff()))) {
			query.connect();
			var first = query.getAsyncApi().whoAmI();
			var waiting = query.getAsyncApi().instance().version();
			var failure = assertThrows(TS3TimeoutException.class, () -> FutureAssertions.read(waiting, 1, TimeUnit.SECONDS));
			assertEquals(TS3TimeoutException.Phase.QUEUE_WAIT, failure.getPhase());
			assertEquals("version", failure.getCommandName());
			assertFalse(failure.isOutcomeUnknown());
			release.countDown();
			assertNotNull(FutureAssertions.read(first));
			query.exit(); peer.await();
		} finally { release.countDown(); }
	}

	@Test void responseTimeoutHasUnknownOutcome() throws Exception {
		try (var peer = new FakeServerQuery(p -> {
			p.expect("whoami");
			p.expect(null);
		}); var query = new TS3Query(config(peer.port()))) {
			query.connect();
			var failure = assertThrows(TS3TimeoutException.class, () -> FutureAssertions.read(query.getAsyncApi().whoAmI(), 2, TimeUnit.SECONDS));
			assertEquals(TS3TimeoutException.Phase.RESPONSE, failure.getPhase());
			assertEquals("whoami", failure.getCommandName());
			assertTrue(failure.isOutcomeUnknown());
			peer.await();
		}
	}

	@Test void transportFailuresShareOneCategory() throws Exception {
		int closedPort;
		try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { closedPort = socket.getLocalPort(); }
		try (var query = new TS3Query(config(closedPort))) {
			TS3TransportException connect = assertThrows(TS3ConnectionFailedException.class, query::connect);
			assertFalse(connect.isOutcomeUnknown());
		}
		var query = new TS3Query(config(closedPort));
		query.close();
		TS3TransportException closed = assertThrows(TS3QueryShutDownException.class, () -> query.getApi().whoAmI());
		assertFalse(closed.isOutcomeUnknown());
	}

	@Test void unsupportedDataIsReportedThroughTheLibraryHierarchy() {
		TS3Exception failure = assertThrows(TS3UnsupportedOperationException.class, () -> new ClientInfo(1, Map.of()).isTalking());
		assertFalse(failure.isOutcomeUnknown());
	}
}
