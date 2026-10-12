package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.*;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class FileTransferApiTest {
    private static TS3Config config(int port) {
        return new TS3Config().setHost("127.0.0.1").setQueryPort(port).setFloodRate(TS3Query.FloodRate.UNLIMITED);
    }
    private static void response(FakeServerQuery peer, int filePort, String fields) throws Exception {
        peer.write("clientftfid=0 serverftfid=1 ftkey=k port=" + filePort + " " + fields + "\nerror id=0 msg=ok\n");
    }
    private static void nextQuery(FakeServerQuery peer) throws Exception {
        peer.expect("whoami"); peer.write("client_id=42\nerror id=0 msg=ok\n"); peer.finishQuit();
    }
    private static FileTransferConfig countingConfig(AtomicInteger calls, int maxInMemoryBytes) {
        return new FileTransferConfig(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1), maxInMemoryBytes,
            (host, ip, port) -> { calls.incrementAndGet(); return new InetSocketAddress(host, port); });
    }
    private static void recovered(TS3Query query) throws Exception {
        FutureAssertions.awaitCommandAdmission(query);
        assertEquals(42, query.getAsyncApi().whoAmI().get(2, TimeUnit.SECONDS).getId());
        query.exit();
    }

    @Test void iconBufferingIsBoundedCancellableAndCallerOwned() throws Exception {
        var defaults = FileTransferConfig.defaults();
        try (var query = new TS3Query(config(1).setCommandCapacity(1).setFileTransferConfig(new FileTransferConfig(
                defaults.connectTimeout(), defaults.readTimeout(), defaults.transferTimeout(), 16, defaults.endpointResolver())))) {
            assertThrows(TS3FileTransferFailedException.class, () -> CommandFutures.join(
                query.getAsyncApi().uploadIcon(InputStream.nullInputStream(), 17)));
            FutureAssertions.awaitCommandAdmission(query);
            var stream = new CallerStreams.BlockingInput();
            var request = query.getAsyncApi().uploadIcon(stream, 1);
            assertTrue(stream.entered.await(2, TimeUnit.SECONDS));
            assertThrows(TS3QueueFullException.class, () -> CommandFutures.join(query.getAsyncApi().whoAmI()));
            assertTrue(request.cancel(false)); FutureAssertions.awaitCommandAdmission(query); assertFalse(stream.closed);
        }
    }

    @Test void startupIconPreparationUsesStartupAdmissionEvenWhenApplicationQueueIsFull() throws Exception {
        byte[] bytes = {7}; long iconId = FileTransferHelper.getIconId(bytes);
        var received = new CompletableFuture<byte[]>();
        try (var files = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var peer = new FakeServerQuery(p -> {
                 p.expect("ftinitupload clientftfid=0 name=\\/icon_" + iconId + " cid=0 cpw= size=1 overwrite=0 resume=0 proto=0");
                 response(p, files.getLocalPort(), "seekpos=0"); nextQuery(p);
             }); var query = new TS3Query(config(peer.port()).setCommandCapacity(1).setConnectionHandler(
                 new com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler() {
                     @Override public void onConnect(TS3Api api) {
                         assertEquals(iconId, api.uploadIcon(new ByteArrayInputStream(bytes), bytes.length));
                     }
                     @Override public void onDisconnect(TS3Query query) { }
                 }))) {
            files.setSoTimeout(2000);
            var server = Thread.ofVirtual().start(() -> {
                try (var socket = files.accept()) {
                    socket.setSoTimeout(2000); assertEquals('k', socket.getInputStream().read());
                    received.complete(socket.getInputStream().readAllBytes());
                } catch (Throwable failure) { received.completeExceptionally(failure); }
            });
            var pending = query.getAsyncApi().whoAmI(); assertEquals(0, query.commandAdmission(true).availablePermits());
            query.connect(); assertEquals(42, pending.get(2, TimeUnit.SECONDS).getId());
            assertArrayEquals(bytes, received.get(2, TimeUnit.SECONDS)); FutureAssertions.awaitCommandAdmission(query);
            query.exit(); peer.await(); server.join(2000); assertFalse(server.isAlive());
        }
    }

    @Test void completedIconPreparationsDoNotAccumulateDelayedTimers() throws Exception {
        try (var query = new TS3Query(config(1))) {
            for (int count = 0; count < 16; count++) {
                assertThrows(TS3FileTransferFailedException.class, () -> CommandFutures.join(
                    query.getAsyncApi().uploadIcon(InputStream.nullInputStream(), 1)));
            }
            FutureAssertions.awaitCommandAdmission(query);
            var field = TS3Query.class.getDeclaredField("deadlines"); field.setAccessible(true);
            var scheduler = (ScheduledThreadPoolExecutor) field.get(query);
            assertTrue(scheduler.getQueue().size() <= 1,
                "Only the periodic query deadline task may remain; completed file timers must be removed");
        }
    }

    @Test void unsentCancellationDoesNotResolveOrOpenFileSocket() throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new FakeServerQuery(FileTransferApiTest::nextQuery);
             var query = new TS3Query(config(peer.port()).setFileTransferConfig(countingConfig(calls, 10)))) {
            var transfer = query.getAsyncApi().downloadFile(new ByteArrayOutputStream(), "/file", 0);
            assertTrue(transfer.cancel(false)); query.connect(); recovered(query); peer.await(); assertEquals(0, calls.get());
        }
    }

    @Test void cancellationDuringInitializationConsumesLateResponseWithoutOpeningSocket() throws Exception {
        var received = new CountDownLatch(1); var release = new CountDownLatch(1); var calls = new AtomicInteger();
        try (var peer = new FakeServerQuery(p -> {
            pCommand(p, "ftinitdownload"); received.countDown(); assertTrue(release.await(3, TimeUnit.SECONDS));
            response(p, 30033, "size=1"); nextQuery(p);
        }); var query = new TS3Query(config(peer.port()).setFileTransferConfig(countingConfig(calls, 10)))) {
            query.connect(); var transfer = query.getAsyncApi().downloadFile(new ByteArrayOutputStream(), "/file", 0);
            assertTrue(received.await(2, TimeUnit.SECONDS)); assertTrue(transfer.cancel(true)); release.countDown();
            recovered(query); peer.await(); assertEquals(0, calls.get());
        } finally { release.countDown(); }
    }

    private static void pCommand(FakeServerQuery peer, String name) throws Exception {
        if (name.equals("ftinitdownload")) peer.expect("ftinitdownload clientftfid=0 name=\\/file cid=0 cpw= seekpos=0 proto=0");
        else peer.expect("ftinitupload clientftfid=0 name=\\/file cid=0 cpw= size=2 overwrite=1 resume=0 proto=0");
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void cancellationOrQueryCloseAbortsActiveReadAndPreservesOutput(boolean close) throws Exception {
        var authenticated = new CountDownLatch(1); var eof = new CompletableFuture<Integer>();
        try (var files = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var peer = new FakeServerQuery(p -> {
                 pCommand(p, "ftinitdownload"); response(p, files.getLocalPort(), "size=1");
                 if (!close) nextQuery(p); else eof.get(3, TimeUnit.SECONDS);
             }); var query = new TS3Query(config(peer.port()))) {
            files.setSoTimeout(2000);
            var server = Thread.ofVirtual().start(() -> {
                try (var socket = files.accept()) {
                    socket.setSoTimeout(2000); assertEquals('k', socket.getInputStream().read());
                    authenticated.countDown(); eof.complete(socket.getInputStream().read());
                } catch (Throwable failure) { eof.completeExceptionally(failure); }
            });
            var output = new ByteArrayOutputStream() {
                boolean closed;
                @Override public void close() { closed = true; }
            };
            query.connect(); var transfer = query.getAsyncApi().downloadFile(output, "/file", 0);
            assertTrue(authenticated.await(2, TimeUnit.SECONDS));
            if (close) {
                query.close(); assertThrows(TS3FileTransferFailedException.class, () -> CommandFutures.join(transfer));
            } else {
                assertTrue(transfer.cancel(false)); assertTrue(transfer.isCancelled()); recovered(query); peer.await();
            }
            assertEquals(-1, eof.get(2, TimeUnit.SECONDS)); assertFalse(output.closed); assertEquals(0, output.size());
            server.join(2000); assertFalse(server.isAlive());
        }
    }

    @Test void cancellationInterruptsCallerUploadReadAndClosesSocket() throws Exception {
        var eof = new CompletableFuture<byte[]>();
        try (var files = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var peer = new FakeServerQuery(p -> { pCommand(p, "ftinitupload"); response(p, files.getLocalPort(), "seekpos=0"); nextQuery(p); });
             var query = new TS3Query(config(peer.port()))) {
            files.setSoTimeout(2000);
            var server = Thread.ofVirtual().start(() -> {
                try (var socket = files.accept()) {
                    socket.setSoTimeout(2000); assertEquals('k', socket.getInputStream().read()); eof.complete(socket.getInputStream().readAllBytes());
                } catch (Throwable failure) { eof.completeExceptionally(failure); }
            });
            var input = new CallerStreams.BlockingInput();
            query.connect(); var transfer = query.getAsyncApi().uploadFile(input, 2, "/file", true, 0);
            assertTrue(input.entered.await(2, TimeUnit.SECONDS)); assertTrue(transfer.cancel(false));
            recovered(query); peer.await(); assertFalse(input.closed); assertArrayEquals(new byte[0], eof.get(2, TimeUnit.SECONDS));
            server.join(2000); assertFalse(server.isAlive());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"size=-1", "size=17", "size=not_a_number", "size=9223372036854775808",
        "size=1 port=0", "size=1 port=65536", "size=1 port=bad", "status=invalid", "status=2050 msg=file_exists"})
    void malformedSizeMemoryLimitAndServerRejectionNeverResolve(String fields) throws Exception {
        var calls = new AtomicInteger();
        try (var peer = new FakeServerQuery(p -> { pCommand(p, "ftinitdownload"); response(p, 30033, fields); nextQuery(p); });
             var query = new TS3Query(config(peer.port()).setFileTransferConfig(countingConfig(calls, 16)))) {
            query.connect(); var transfer = query.getAsyncApi().downloadFileDirect("/file", 0);
            if (fields.startsWith("status=2050")) assertEquals(2050, assertThrows(TS3CommandFailedException.class,
                () -> CommandFutures.join(transfer)).getError().getId());
            else assertThrows(TS3FileTransferFailedException.class, () -> CommandFutures.join(transfer));
            recovered(query); peer.await(); assertEquals(0, calls.get());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void completeDeadlineOrCloseSurvivesUncooperativeResolverAndRetainsAdmission(boolean close) throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var peer = new FakeServerQuery(p -> { pCommand(p, "ftinitdownload"); response(p, 30033, "size=1"); if (!close) nextQuery(p); else release.await(3, TimeUnit.SECONDS); });
             var query = new TS3Query(config(peer.port()).setCommandCapacity(1).setFileTransferConfig(new FileTransferConfig(
                 Duration.ofSeconds(1), Duration.ofSeconds(1), close ? Duration.ofSeconds(3) : Duration.ofMillis(100), 16, (host, ip, port) -> {
                     entered.countDown(); boolean interrupted = false;
                     while (release.getCount() > 0) {
                         try { release.await(); } catch (InterruptedException failure) { interrupted = true; }
                     }
                     if (interrupted) Thread.currentThread().interrupt();
                     return new InetSocketAddress(host, port);
                 })))) {
            query.connect(); var transfer = query.getAsyncApi().downloadFileDirect("/file", 0);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            if (close) query.close();
            var failure = assertThrows(ExecutionException.class, () -> transfer.get(2, TimeUnit.SECONDS));
            assertInstanceOf(TS3FileTransferFailedException.class, failure.getCause());
            if (!close) {
                assertInstanceOf(SocketTimeoutException.class, failure.getCause().getCause());
                assertThrows(TS3QueueFullException.class, () -> CommandFutures.join(query.getAsyncApi().whoAmI()));
            } else assertEquals(0, query.commandAdmission(true).availablePermits());
            release.countDown();
            if (!close) recovered(query);
            peer.await();
        } finally { release.countDown(); }
    }
}
