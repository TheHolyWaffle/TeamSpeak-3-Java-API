package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.CallerStreams.Input;
import com.github.theholywaffle.teamspeak3.CallerStreams.Output;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class FileTransferCompatibilityIT {
    private static void eventually(Callable<Boolean> condition, String message) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.call()) {
            assertTrue(System.nanoTime() < until, message); Thread.sleep(20);
        }
    }
    private static TS3CommandFailedException rejected(CompletableFuture<?> request) {
        var error = assertThrows(TS3CommandFailedException.class, () -> CommandFutures.join(request));
        assertFalse(error.getError().isSuccessful()); return error;
    }

    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void publicTransfersAndResumeThroughMappedEndpoint(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>(); var resolutions = new AtomicInteger();
        try (var server = new TeamSpeakContainer()) {
            server.addExposedPort(30033); server.start();
            var settings = server.config(target.protocol).setFileTransferConfig(new FileTransferConfig(
                Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(15), 1024,
                (host, advertised, port) -> {
                    assertEquals(server.getHost(), host); assertEquals(30033, port); resolutions.incrementAndGet();
                    return new InetSocketAddress(server.getHost(), server.getMappedPort(port));
                }));
            try (var query = new TS3Query(settings)) {
                query.connect(); var api = query.getApi(); var async = query.getAsyncApi(); api.selectVirtualServerById(1);
                server.record(target + "-reliable-files-server", api.getVersion());
                for (String command : List.of("ftinitupload", "ftinitdownload", "ftstop"))
                    evidence.add("help " + command + "\n" + api.executeRawCommand("help " + command).getRawResponse());
                evidence.add("query=" + target + ", fileSocket=plaintext TCP, mappedPort=" + server.getMappedPort(30033));
                int cid = api.createChannel("reliable-files-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                byte[] bytes = new byte[65537]; new Random(442).nextBytes(bytes);
                var source = new Input(bytes); api.uploadFile(source, bytes.length, "/bytes.bin", true, cid);
                assertFalse(source.closed); eventually(() -> api.getFileInfo("/bytes.bin", cid).getFileSize() == bytes.length, "Upload size");
                var output = new Output(); assertEquals(bytes.length, api.downloadFile(output, "/bytes.bin", cid));
                assertArrayEquals(bytes, output.toByteArray()); assertFalse(output.closed); assertFalse(output.flushed);
                assertThrows(TS3FileTransferFailedException.class, () -> api.downloadFileDirect("/bytes.bin", cid));
                assertEquals(bytes.length, api.getFileInfo("/bytes.bin", cid).getFileSize());
                evidence.add("65537-byte public upload/download and 1024-byte direct limit passed");

                api.uploadFileDirect(new byte[0], "/empty.bin", true, cid);
                eventually(() -> api.getFileInfo("/empty.bin", cid).getFileSize() == 0, "Empty upload");
                assertArrayEquals(new byte[0], api.downloadFileDirect("/empty.bin", cid));
                var atEnd = new Output(); assertEquals(0, api.downloadFile(atEnd, "/bytes.bin", cid, "", bytes.length));
                assertEquals(0, atEnd.size());
                int offset = 12345; var suffix = new Output();
                assertEquals(bytes.length - offset, api.downloadFile(suffix, "/bytes.bin", cid, "", offset));
                assertArrayEquals(Arrays.copyOfRange(bytes, offset, bytes.length), suffix.toByteArray());
                evidence.add("empty files and download seekpos (including EOF) passed");

                var missing = rejected(async.downloadFile(new Output(), "/missing.bin", cid));
                var existing = rejected(async.uploadFile(new Input(new byte[]{1}), 1, "/bytes.bin", false, cid));
                evidence.add("missing download error=" + missing.getError().getId() + "; overwrite rejection=" + existing.getError().getId());
                var recovered = new Output(); assertEquals(bytes.length, api.downloadFile(recovered, "/bytes.bin", cid));
                assertArrayEquals(bytes, recovered.toByteArray());
                var truncated = new Input(new byte[]{1,2,3});
                assertThrows(TS3FileTransferFailedException.class, () -> api.uploadFile(truncated, 7, "/resume.bin", true, cid));
                assertFalse(truncated.closed);
                eventually(() -> api.getFileInfo("/resume.bin", cid).getFileSize() == 3, "Truncated upload prefix is retained");
                byte[] full = {1,2,3,4,5,6,7};
                var resumed = new Input(full); api.uploadFile(resumed, full.length, "/resume.bin", false, cid, "", true);
                assertFalse(resumed.closed);
                eventually(() -> api.getFileInfo("/resume.bin", cid).getFileSize() == full.length, "Resumed size");
                assertArrayEquals(full, api.downloadFileDirect("/resume.bin", cid));
                evidence.add("truncated upload fails; resume skips server-acknowledged 3-byte prefix and exact final bytes pass");

                var uploadEntered = new CountDownLatch(1);
                var cancelledSource = new InputStream() {
                    boolean prefix; volatile boolean closed;
                    @Override public int read() throws IOException { throw new IOException("bulk reads required"); }
                    @Override public int read(byte[] buffer, int start, int length) throws IOException {
                        if (!prefix) { prefix = true; Arrays.fill(buffer, start, start + Math.min(3, length), (byte)9); return Math.min(3, length); }
                        uploadEntered.countDown();
                        try { new CountDownLatch(1).await(); throw new AssertionError(); }
                        catch (InterruptedException failure) { throw new InterruptedIOException("cancelled caller read"); }
                    }
                    @Override public void close() { closed = true; }
                };
                var upload = async.uploadFile(cancelledSource, 7, "/cancel.bin", true, cid);
                assertTrue(uploadEntered.await(5, TimeUnit.SECONDS)); assertTrue(upload.cancel(false));
                FutureAssertions.awaitCommandAdmission(query);
                eventually(() -> api.getFileTransfers().stream().noneMatch(row -> row.isActive()), "Cancelled upload stops");
                assertFalse(cancelledSource.closed); assertTrue(upload.isCancelled());
                eventually(() -> api.getFileInfo("/cancel.bin", cid).getFileSize() == 3, "Cancelled prefix retained");
                api.uploadFile(new Input(new byte[]{9,9,9,4,5,6,7}), 7, "/cancel.bin", false, cid, "", true);
                eventually(() -> api.getFileInfo("/cancel.bin", cid).getFileSize() == 7, "Cancelled upload can resume");
                assertArrayEquals(new byte[]{9,9,9,4,5,6,7}, api.downloadFileDirect("/cancel.bin", cid));

                var downloadEntered = new CountDownLatch(1);
                var cancelledOutput = new Output() {
                    @Override public synchronized void write(byte[] buffer, int offset, int length) {
                        downloadEntered.countDown();
                        try { new CountDownLatch(1).await(); throw new AssertionError(); }
                        catch (InterruptedException expected) { Thread.currentThread().interrupt(); }
                    }
                };
                var download = async.downloadFile(cancelledOutput, "/bytes.bin", cid);
                assertTrue(downloadEntered.await(5, TimeUnit.SECONDS)); assertTrue(download.cancel(true));
                FutureAssertions.awaitCommandAdmission(query);
                assertFalse(cancelledOutput.closed); assertFalse(cancelledOutput.flushed); assertEquals(0, cancelledOutput.size());
                assertTrue(download.isCancelled());
                assertFalse(api.getVersion().getVersion().isBlank());
                evidence.add("active upload/download cancellation, caller stream ownership, retained-prefix resume and query recovery passed");
                byte[] icon = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXh8AAAAASUVORK5CYII=");
                var iconSource = new Input(icon); long iconId = api.uploadIcon(iconSource, icon.length);
                assertEquals(FileTransferHelper.getIconId(icon), iconId); assertFalse(iconSource.closed);
                eventually(() -> api.getIconList().stream().anyMatch(row -> row.getIconId() == iconId && row.getFileSize() == icon.length), "Icon size");
                assertArrayEquals(icon, api.downloadIconDirect(iconId));
                assertEquals(iconId, api.uploadIconDirect(icon)); api.deleteIcons(iconId);
                evidence.add("bounded asynchronous icon stream buffering, exact icon bytes, duplicate-icon recovery and ownership passed");
                assertTrue(resolutions.get() >= 10); evidence.add("endpoint resolver calls=" + resolutions.get());
                api.deleteChannel(cid, true); query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-reliable-files.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }
}
