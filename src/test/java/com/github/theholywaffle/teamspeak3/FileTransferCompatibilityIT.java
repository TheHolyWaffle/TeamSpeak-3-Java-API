package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.UploadMode;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
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
import static com.github.theholywaffle.teamspeak3.FutureAssertions.eventually;
import static org.junit.jupiter.api.Assertions.*;

class FileTransferCompatibilityIT {
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
                server.record(target + "-reliable-files-server", api.instance().version());
                for (String command : List.of("ftinitupload", "ftinitdownload", "ftstop"))
                    evidence.add("help " + command + "\n" + api.executeRawCommand("help " + command).getRawResponse());
                evidence.add("query=" + target + ", fileSocket=plaintext TCP, mappedPort=" + server.getMappedPort(30033));
                int cid = api.channels().create("reliable-files-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                byte[] bytes = new byte[65537]; new Random(442).nextBytes(bytes);
                var source = new Input(bytes); api.files().upload(FileLocation.of(cid, "/bytes.bin"), source, bytes.length, UploadMode.OVERWRITE);
                assertFalse(source.closed); eventually(() -> api.files().info(FileLocation.of(cid, "/bytes.bin")).getFileSize() == bytes.length, "Upload size");
                var output = new Output(); assertEquals(bytes.length, api.files().download(FileLocation.of(cid, "/bytes.bin"), output));
                assertArrayEquals(bytes, output.toByteArray()); assertFalse(output.closed); assertFalse(output.flushed);
                assertThrows(TS3FileTransferFailedException.class, () -> api.files().downloadBytes(FileLocation.of(cid, "/bytes.bin")));
                assertEquals(bytes.length, api.files().info(FileLocation.of(cid, "/bytes.bin")).getFileSize());
                evidence.add("65537-byte public upload/download and 1024-byte direct limit passed");

                api.files().upload(FileLocation.of(cid, "/empty.bin"), new byte[0], UploadMode.OVERWRITE);
                eventually(() -> api.files().info(FileLocation.of(cid, "/empty.bin")).getFileSize() == 0, "Empty upload");
                assertArrayEquals(new byte[0], api.files().downloadBytes(FileLocation.of(cid, "/empty.bin")));
                var atEnd = new Output(); assertEquals(0, api.files().download(FileLocation.of(cid, "/bytes.bin").withChannelPassword(""), atEnd, bytes.length));
                assertEquals(0, atEnd.size());
                int offset = 12345; var suffix = new Output();
                assertEquals(bytes.length - offset, api.files().download(FileLocation.of(cid, "/bytes.bin").withChannelPassword(""), suffix, offset));
                assertArrayEquals(Arrays.copyOfRange(bytes, offset, bytes.length), suffix.toByteArray());
                evidence.add("empty files and download seekpos (including EOF) passed");

                var missing = rejected(async.files().download(FileLocation.of(cid, "/missing.bin"), new Output()));
                var existing = rejected(async.files().upload(FileLocation.of(cid, "/bytes.bin"), new Input(new byte[]{1}), 1, UploadMode.CREATE));
                evidence.add("missing download error=" + missing.getError().getId() + "; overwrite rejection=" + existing.getError().getId());
                var recovered = new Output(); assertEquals(bytes.length, api.files().download(FileLocation.of(cid, "/bytes.bin"), recovered));
                assertArrayEquals(bytes, recovered.toByteArray());
                var truncated = new Input(new byte[]{1,2,3});
                assertThrows(TS3FileTransferFailedException.class, () -> api.files().upload(FileLocation.of(cid, "/resume.bin"), truncated, 7, UploadMode.OVERWRITE));
                assertFalse(truncated.closed);
                eventually(() -> api.files().info(FileLocation.of(cid, "/resume.bin")).getFileSize() == 3, "Truncated upload prefix is retained");
                byte[] full = {1,2,3,4,5,6,7};
                var resumed = new Input(full); api.files().upload(FileLocation.of(cid, "/resume.bin").withChannelPassword(""), resumed, full.length, UploadMode.RESUME);
                assertFalse(resumed.closed);
                eventually(() -> api.files().info(FileLocation.of(cid, "/resume.bin")).getFileSize() == full.length, "Resumed size");
                assertArrayEquals(full, api.files().downloadBytes(FileLocation.of(cid, "/resume.bin")));
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
                var upload = async.files().upload(FileLocation.of(cid, "/cancel.bin"), cancelledSource, 7, UploadMode.OVERWRITE);
                assertTrue(uploadEntered.await(5, TimeUnit.SECONDS)); assertTrue(upload.cancel(false));
                FutureAssertions.awaitCommandAdmission(query);
                eventually(() -> api.files().transfers().stream().noneMatch(row -> row.isActive()), "Cancelled upload stops");
                assertFalse(cancelledSource.closed); assertTrue(upload.isCancelled());
                eventually(() -> api.files().info(FileLocation.of(cid, "/cancel.bin")).getFileSize() == 3, "Cancelled prefix retained");
                api.files().upload(FileLocation.of(cid, "/cancel.bin").withChannelPassword(""), new Input(new byte[]{9,9,9,4,5,6,7}), 7, UploadMode.RESUME);
                eventually(() -> api.files().info(FileLocation.of(cid, "/cancel.bin")).getFileSize() == 7, "Cancelled upload can resume");
                assertArrayEquals(new byte[]{9,9,9,4,5,6,7}, api.files().downloadBytes(FileLocation.of(cid, "/cancel.bin")));

                var downloadEntered = new CountDownLatch(1);
                var cancelledOutput = new Output() {
                    @Override public synchronized void write(byte[] buffer, int offset, int length) {
                        downloadEntered.countDown();
                        try { new CountDownLatch(1).await(); throw new AssertionError(); }
                        catch (InterruptedException expected) { Thread.currentThread().interrupt(); }
                    }
                };
                var download = async.files().download(FileLocation.of(cid, "/bytes.bin"), cancelledOutput);
                assertTrue(downloadEntered.await(5, TimeUnit.SECONDS)); assertTrue(download.cancel(true));
                FutureAssertions.awaitCommandAdmission(query);
                assertFalse(cancelledOutput.closed); assertFalse(cancelledOutput.flushed); assertEquals(0, cancelledOutput.size());
                assertTrue(download.isCancelled());
                assertFalse(api.instance().version().getVersion().isBlank());
                evidence.add("active upload/download cancellation, caller stream ownership, retained-prefix resume and query recovery passed");
                byte[] icon = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXh8AAAAASUVORK5CYII=");
                var iconSource = new Input(icon); long iconId = api.files().uploadIcon(iconSource, icon.length);
                assertEquals(FileTransferHelper.getIconId(icon), iconId); assertFalse(iconSource.closed);
                eventually(() -> api.files().icons().stream().anyMatch(row -> row.getIconId() == iconId && row.getFileSize() == icon.length), "Icon size");
                assertArrayEquals(icon, api.files().downloadIcon(iconId));
                assertEquals(iconId, api.files().uploadIcon(icon)); api.files().deleteIcons(iconId);
                evidence.add("bounded asynchronous icon stream buffering, exact icon bytes, duplicate-icon recovery and ownership passed");
                assertTrue(resolutions.get() >= 10); evidence.add("endpoint resolver calls=" + resolutions.get());
                api.channels().delete(cid, true); query.exit();
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-reliable-files.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }
}
