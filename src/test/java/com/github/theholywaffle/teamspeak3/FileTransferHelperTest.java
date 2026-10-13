package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.CallerStreams.Input;
import com.github.theholywaffle.teamspeak3.CallerStreams.Output;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(10)
class FileTransferHelperTest {
    private static final Duration SECOND = Duration.ofSeconds(1);

    private static FileTransferParameters parameters(int port, long size, long position) {
        return new FileTransferParameters(Map.of(
                "ip", "127.0.0.1", "port", "" + port, "ftkey", "k", "size", "" + size, "seekpos", "" + position));
    }

    private static final class Fixture implements AutoCloseable {
        final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        final FileTransferHelper helper;

        Fixture(Duration read, Duration total) throws IOException {
            server.setSoTimeout(2000);
            helper = new FileTransferHelper(
                    "127.0.0.1",
                    new FileTransferConfig(SECOND, read, total, 1024, FileTransferEndpointResolver.advertised()),
                    timer);
        }

        Fixture() throws IOException {
            this(SECOND, Duration.ofSeconds(3));
        }

        FileTransferHelper.Operation operation() throws IOException {
            var operation = helper.newOperation();
            operation.start(() -> {});
            return operation;
        }

        Socket accept() throws IOException {
            var socket = server.accept();
            socket.setSoTimeout(2000);
            assertEquals('k', socket.getInputStream().read());
            return socket;
        }

        @Override
        public void close() throws Exception {
            helper.close();
            server.close();
            timer.shutdownNow();
            assertTrue(timer.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void advertisedHostSelectionAndMappedPort() {
        var resolver = FileTransferEndpointResolver.advertised();
        assertEquals(
                "192.0.2.1", resolver.resolve("127.0.0.1", "192.0.2.1", 30033).getHostString());
        assertEquals(
                "192.0.2.1",
                resolver.resolve("127.0.0.1", "192.0.2.1,192.0.2.2", 30033).getHostString());
        for (String raw :
                new String[] {null, "", "0.0.0.0", "0.0.0.0,192.0.2.1", "::", "[::]", "0:0:0:0:0:0:0:0", ",192.0.2.1"})
            assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", raw, 30033).getHostString());
        assertTrue(resolver.resolve("127.0.0.1", "::1", 30033).getAddress().isLoopbackAddress());
        assertEquals(
                "0.0.0.0.example.invalid",
                resolver.resolve("127.0.0.1", "0.0.0.0.example.invalid", 30033).getHostString());
        assertEquals(30033, resolver.resolve("127.0.0.1", "", 30033).getPort());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 65537})
    void exactDownloadAndCallerOutputOwnership(int length) throws Exception {
        byte[] bytes = new byte[length];
        new java.util.Random(442).nextBytes(bytes);
        try (var fixture = new Fixture()) {
            var sent = CompletableFuture.runAsync(() -> {
                try (var socket = fixture.accept()) {
                    socket.getOutputStream().write(bytes);
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var output = new Output();
            try (var operation = fixture.operation()) {
                fixture.helper.downloadFile(output, parameters(fixture.server.getLocalPort(), length, 0), 0, operation);
            }
            sent.get(2, TimeUnit.SECONDS);
            assertArrayEquals(bytes, output.toByteArray());
            assertFalse(output.closed);
            assertFalse(output.flushed);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 65537})
    void exactUploadLeavesExtraSourceBytesAndStreamOpen(int length) throws Exception {
        byte[] bytes = new byte[length + 1];
        new java.util.Random(442).nextBytes(bytes);
        try (var fixture = new Fixture()) {
            var received = CompletableFuture.supplyAsync(() -> {
                try (var socket = fixture.accept()) {
                    return socket.getInputStream().readAllBytes();
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var input = new Input(bytes);
            try (var operation = fixture.operation()) {
                fixture.helper.uploadFile(
                        input, length, parameters(fixture.server.getLocalPort(), length, 0), false, operation);
            }
            assertArrayEquals(java.util.Arrays.copyOf(bytes, length), received.get(2, TimeUnit.SECONDS));
            assertEquals(1, input.available());
            assertFalse(input.closed);
        }
    }

    @Test
    void truncatedDownloadAndUploadFailWithoutClosingCallerStreams() throws Exception {
        try (var fixture = new Fixture()) {
            var sent = CompletableFuture.runAsync(() -> {
                try (var socket = fixture.accept()) {
                    socket.getOutputStream().write(7);
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var output = new Output();
            try (var operation = fixture.operation()) {
                assertThrows(
                        IOException.class,
                        () -> fixture.helper.downloadFile(
                                output, parameters(fixture.server.getLocalPort(), 2, 0), 0, operation));
            }
            sent.get(2, TimeUnit.SECONDS);
            assertArrayEquals(new byte[] {7}, output.toByteArray());
            assertFalse(output.closed);
        }
        try (var fixture = new Fixture()) {
            var received = CompletableFuture.supplyAsync(() -> {
                try (var socket = fixture.accept()) {
                    return socket.getInputStream().readAllBytes();
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var input = new Input(new byte[] {7});
            try (var operation = fixture.operation()) {
                assertThrows(
                        IOException.class,
                        () -> fixture.helper.uploadFile(
                                input, 2, parameters(fixture.server.getLocalPort(), 2, 0), false, operation));
            }
            assertArrayEquals(new byte[] {7}, received.get(2, TimeUnit.SECONDS));
            assertFalse(input.closed);
        }
    }

    @Test
    void readDeadlineFailsSilentSocket() throws Exception {
        try (var fixture = new Fixture(Duration.ofMillis(100), Duration.ofSeconds(3))) {
            var eof = CompletableFuture.supplyAsync(() -> {
                try (var socket = fixture.accept()) {
                    return socket.getInputStream().read();
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            try (var operation = fixture.operation()) {
                assertThrows(
                        SocketTimeoutException.class,
                        () -> fixture.helper.downloadFile(
                                new Output(), parameters(fixture.server.getLocalPort(), 1, 0), 0, operation));
            }
            assertEquals(-1, eof.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void totalDeadlineStopsBlockedSocketWrite() throws Exception {
        var authenticated = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var fixture = new Fixture(SECOND, Duration.ofMillis(200))) {
            var peer = CompletableFuture.runAsync(() -> {
                try (var socket = fixture.accept()) {
                    authenticated.countDown();
                    release.await(3, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new CompletionException(failure);
                }
            });
            var stopped = new CompletableFuture<Throwable>();
            var worker = Thread.ofVirtual().start(() -> {
                try (var operation = fixture.operation()) {
                    fixture.helper.uploadFile(
                            new InputStream() {
                                @Override
                                public int read() {
                                    return 1;
                                }

                                @Override
                                public int read(byte[] bytes, int offset, int count) {
                                    return count;
                                }
                            },
                            Long.MAX_VALUE,
                            parameters(fixture.server.getLocalPort(), 0, 0),
                            false,
                            operation);
                    stopped.complete(null);
                } catch (Throwable failure) {
                    stopped.complete(failure);
                }
            });
            try {
                assertTrue(authenticated.await(2, TimeUnit.SECONDS));
                assertInstanceOf(IOException.class, stopped.get(2, TimeUnit.SECONDS));
                worker.join(2000);
                assertFalse(worker.isAlive());
            } finally {
                release.countDown();
            }
            peer.get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
    }

    @Test
    void lengthsMemoryLimitsAndNoProgressFailBeforeAllocationOrSocket() throws Exception {
        assertThrows(
                IOException.class, () -> FileTransferHelper.readFully(InputStream.nullInputStream(), -1, 1024, null));
        assertThrows(
                IOException.class,
                () -> FileTransferHelper.readFully(InputStream.nullInputStream(), Long.MAX_VALUE, 1024, null));
        assertArrayEquals(new byte[0], FileTransferHelper.readFully(InputStream.nullInputStream(), 0, 0, null));
        assertThrows(
                IOException.class, () -> FileTransferHelper.readFully(InputStream.nullInputStream(), 1, 1024, null));
        var noProgress = new InputStream() {
            @Override
            public int read() {
                return 0;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) {
                return 0;
            }
        };
        assertThrows(IOException.class, () -> FileTransferHelper.readFully(noProgress, 1, 1024, null));
        try (var fixture = new Fixture();
                var operation = fixture.operation()) {
            assertThrows(
                    IOException.class,
                    () -> fixture.helper.downloadFile(new Output(), parameters(1, -1, 0), 0, operation));
            assertThrows(
                    IOException.class,
                    () -> fixture.helper.downloadFile(new Output(), parameters(1, 1, 0), 2, operation));
            assertThrows(
                    IOException.class,
                    () -> fixture.helper.uploadFile(noProgress, -1, parameters(1, 1, 0), false, operation));
            assertThrows(
                    IOException.class,
                    () -> fixture.helper.uploadFile(noProgress, 1, parameters(1, 1, 2), true, operation));
            assertThrows(
                    IOException.class,
                    () -> fixture.helper.uploadFile(noProgress, 1, parameters(1, 1, -1), true, operation));
        }
    }

    @Test
    void resumeConsumesExactPrefixAndDownloadWritesSuffix() throws Exception {
        try (var fixture = new Fixture()) {
            var received = CompletableFuture.supplyAsync(() -> {
                try (var socket = fixture.accept()) {
                    return socket.getInputStream().readAllBytes();
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            try (var operation = fixture.operation()) {
                fixture.helper.uploadFile(
                        new Input(new byte[] {1, 2, 3}),
                        3,
                        parameters(fixture.server.getLocalPort(), 3, 2),
                        true,
                        operation);
            }
            assertArrayEquals(new byte[] {3}, received.get(2, TimeUnit.SECONDS));
        }
        try (var fixture = new Fixture()) {
            var sent = CompletableFuture.runAsync(() -> {
                try (var socket = fixture.accept()) {
                    socket.getOutputStream().write(3);
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var output = new Output();
            try (var operation = fixture.operation()) {
                fixture.helper.downloadFile(output, parameters(fixture.server.getLocalPort(), 3, 0), 2, operation);
            }
            assertArrayEquals(new byte[] {3}, output.toByteArray());
            sent.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void overlongDownloadAndNonprogressingUploadFail() throws Exception {
        try (var fixture = new Fixture()) {
            var sent = CompletableFuture.runAsync(() -> {
                try (var socket = fixture.accept()) {
                    socket.getOutputStream().write(new byte[] {1, 2});
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            var output = new Output();
            try (var operation = fixture.operation()) {
                assertThrows(
                        IOException.class,
                        () -> fixture.helper.downloadFile(
                                output, parameters(fixture.server.getLocalPort(), 1, 0), 0, operation));
            }
            sent.get(2, TimeUnit.SECONDS);
            assertArrayEquals(new byte[] {1}, output.toByteArray());
            assertFalse(output.closed);
        }
        try (var fixture = new Fixture()) {
            var received = CompletableFuture.supplyAsync(() -> {
                try (var socket = fixture.accept()) {
                    return socket.getInputStream().read();
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            });
            try (var operation = fixture.operation()) {
                assertThrows(
                        IOException.class,
                        () -> fixture.helper.uploadFile(
                                new InputStream() {
                                    @Override
                                    public int read() {
                                        return 0;
                                    }

                                    @Override
                                    public int read(byte[] bytes, int offset, int count) {
                                        return 0;
                                    }
                                },
                                1,
                                parameters(fixture.server.getLocalPort(), 1, 0),
                                false,
                                operation));
            }
            assertEquals(-1, received.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void completeDeadlineStopsTricklingDownloadDespiteReadProgress() throws Exception {
        var output = new Output();
        var stopped = new CompletableFuture<Throwable>();
        try (var fixture = new Fixture(Duration.ofSeconds(1), Duration.ofMillis(200))) {
            var peer = Thread.ofVirtual().start(() -> {
                try (var socket = fixture.accept()) {
                    for (int count = 0; count < 1000; count++) {
                        socket.getOutputStream().write(7);
                        Thread.sleep(20);
                    }
                } catch (IOException expectedWhenClientAborts) {
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                }
            });
            var worker = Thread.ofVirtual().start(() -> {
                try (var operation = fixture.operation()) {
                    fixture.helper.downloadFile(
                            output, parameters(fixture.server.getLocalPort(), 1000, 0), 0, operation);
                    stopped.complete(null);
                } catch (Throwable failure) {
                    stopped.complete(failure);
                }
            });
            assertInstanceOf(IOException.class, stopped.get(2, TimeUnit.SECONDS));
            worker.join(2000);
            peer.join(2000);
            assertFalse(worker.isAlive());
            assertFalse(peer.isAlive());
            assertTrue(output.size() > 0 && output.size() < 1000);
            assertFalse(output.closed);
        }
    }

    @Test
    void connectDeadlineBoundsSaturatedLoopbackListener() throws Exception {
        var occupied = new java.util.ArrayList<Socket>();
        try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                var fixture = new Fixture()) {
            boolean saturated = false;
            try {
                for (int count = 0; count < 16; count++) {
                    var socket = new Socket();
                    occupied.add(socket);
                    try {
                        socket.connect(new InetSocketAddress("127.0.0.1", listener.getLocalPort()), 100);
                    } catch (SocketTimeoutException expected) {
                        saturated = true;
                        break;
                    }
                }
                assertTrue(saturated, "Listener backlog must be saturated before testing connect timeout");
                var helper = new FileTransferHelper(
                        "127.0.0.1",
                        new FileTransferConfig(
                                Duration.ofMillis(100),
                                SECOND,
                                Duration.ofSeconds(3),
                                1024,
                                FileTransferEndpointResolver.advertised()),
                        fixture.timer);
                try (helper;
                        var operation = helper.newOperation()) {
                    operation.start(() -> {});
                    assertTimeout(
                            Duration.ofSeconds(1),
                            () -> assertThrows(
                                    SocketTimeoutException.class,
                                    () -> helper.downloadFile(
                                            new Output(), parameters(listener.getLocalPort(), 1, 0), 0, operation)));
                }
            } finally {
                for (var socket : occupied) socket.close();
            }
        }
    }

    @Test
    void configurationIsValidatedAndSnapshotted() {
        var defaults = FileTransferConfig.defaults();
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileTransferConfig(Duration.ZERO, SECOND, SECOND, 10, defaults.endpointResolver()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileTransferConfig(
                        SECOND, Duration.ofSeconds(Long.MAX_VALUE), SECOND, 10, defaults.endpointResolver()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FileTransferConfig(SECOND, SECOND, SECOND, -1, defaults.endpointResolver()));
        assertThrows(NullPointerException.class, () -> new FileTransferConfig(SECOND, SECOND, SECOND, 10, null));
        var builder = new TS3Config();
        var snapshot = builder.build();
        builder.setFileTransferConfig(new FileTransferConfig(SECOND, SECOND, SECOND, 10, defaults.endpointResolver()));
        assertEquals(defaults, snapshot.getFileTransferConfig());
        assertEquals(10, builder.build().getFileTransferConfig().maxInMemoryBytes());
    }
}
