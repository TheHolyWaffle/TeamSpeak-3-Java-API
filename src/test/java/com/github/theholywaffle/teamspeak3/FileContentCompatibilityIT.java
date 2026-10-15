package com.github.theholywaffle.teamspeak3;

import static com.github.theholywaffle.teamspeak3.FutureAssertions.eventually;
import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientFileTransferId;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.ServerFileTransferId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import com.github.theholywaffle.teamspeak3.commands.FileCommands;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FileContentCompatibilityIT {
    @ParameterizedTest
    @EnumSource(ServerQueryCompatibilityIT.Target.class)
    @Timeout(120)
    void contentsAndTransferCommands(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.withFileTransferPort().start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect();
                var api = query.getApi();
                api.selectVirtualServerById(VirtualServerId.of(1));
                for (String command : List.of("ftinitupload", "ftinitdownload", "ftlist", "ftstop", "ftgetfileinfo")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command));
                    evidence.add("help " + command + "\n" + help);
                }
                ChannelId cid = api.channels()
                        .create("file-content-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                byte[] bytes = "actual bytes 日本語 | literal \\s\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                api.files().createDirectory(FileLocation.of(cid, "/contents"));
                String name = "/contents/content 日本語.txt";
                var upload = api.executeRawCommand(FileCommands.ftInitUpload(
                                        1,
                                        FileLocation.of(cid, name).withChannelPassword(""),
                                        bytes.length,
                                        true,
                                        false)
                                .toString())
                        .getFirstResponse()
                        .orElseThrow();
                evidence.add("upload status=" + upload.find("status").orElse("") + " message="
                        + upload.find("msg").orElse(""));
                assertFalse(
                        upload.require("ftkey").isBlank(),
                        "Upload initialization failed: status="
                                + upload.find("status").orElse("") + " message="
                                + upload.find("msg").orElse(""));
                assertEquals(30033, upload.requireInt("port"));
                evidence.add("upload fields="
                        + new java.util.TreeSet<>(upload.getMap().keySet()));
                upload(bytes, mapped(server, upload.getMap()));
                eventually(
                        () -> api.files().info(FileLocation.of(cid, name)).getFileSize() == bytes.length,
                        "Uploaded size must become visible");
                var info = api.files().info(FileLocation.of(cid, name));
                assertTrue(info.isFile());
                assertEquals(name, info.getPath());
                String second = "/contents/second.bin";
                var secondUpload = api.executeRawCommand(FileCommands.ftInitUpload(
                                        6, FileLocation.of(cid, second).withChannelPassword(""), 1, true, false)
                                .toString())
                        .getFirstResponse()
                        .orElseThrow();
                upload(new byte[] {42}, mapped(server, secondUpload.getMap()));
                eventually(
                        () -> api.files().info(FileLocation.of(cid, second)).getFileSize() == 1,
                        "Second uploaded file must become complete");
                var bulk = api.files()
                        .info(java.util.List.of(
                                FileLocation.of(cid, name).withChannelPassword(""),
                                FileLocation.of(cid, second).withChannelPassword("")));
                assertEquals(2, bulk.size());
                var sizes = bulk.stream()
                        .collect(java.util.stream.Collectors.toMap(row -> row.getPath(), row -> row.getFileSize()));
                assertEquals(Map.of(name, (long) bytes.length, second, 1L), sizes);
                evidence.add("positive file-info fields="
                        + new java.util.TreeSet<>(info.getMap().keySet()));
                var download = api.executeRawCommand(FileCommands.ftInitDownload(
                                        2, FileLocation.of(cid, name).withChannelPassword(""), 0)
                                .encoded())
                        .getFirstResponse()
                        .orElseThrow();
                assertEquals(bytes.length, download.requireLong("size"));
                assertArrayEquals(bytes, download(mapped(server, download.getMap())));
                var pending = api.executeRawCommand(FileCommands.ftInitUpload(
                                        3,
                                        FileLocation.of(cid, "/pending.bin").withChannelPassword(""),
                                        100,
                                        true,
                                        false)
                                .toString())
                        .getFirstResponse()
                        .orElseThrow();
                var transfer = ServerFileTransferId.of(pending.requireInt("serverftfid"));
                var listed = api.files().transfers().stream()
                        .filter(row -> row.getServerTransferId().equals(transfer))
                        .toList();
                assertEquals(1, listed.size());
                assertEquals(ClientFileTransferId.of(3), listed.getFirst().getClientTransferId());
                assertEquals(
                        api.whoAmI().getId().orElseThrow(), listed.getFirst().getClientId());
                evidence.add("pending transfer fields="
                        + new java.util.TreeSet<>(listed.getFirst().getMap().keySet()));
                try (var socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(server.getHost(), server.getMappedPort(30033)), 5000);
                    socket.setSoTimeout(5000);
                    socket.getOutputStream()
                            .write(pending.require("ftkey").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    eventually(
                            () -> api.files().transfers().stream()
                                    .anyMatch(row -> row.getServerTransferId().equals(transfer) && row.isActive()),
                            "Authenticated partial upload must become active");
                    api.executeRawCommand("ftstop serverftfid=" + transfer + " delete=1");
                    assertEquals(-1, socket.getInputStream().read(), "Stopped upload must close its data socket");
                }
                assertTrue(api.files().transfers().stream()
                        .noneMatch(row -> row.getServerTransferId().equals(transfer)));
                byte[] icon = java.util.Base64.getDecoder()
                        .decode(
                                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXh8AAAAASUVORK5CYII=");
                var iconId = FileTransferHelper.getIconId(icon);
                var iconUpload = api.executeRawCommand(FileCommands.ftInitUpload(
                                        4,
                                        FileLocation.ofServer(iconId.path()).withChannelPassword(""),
                                        icon.length,
                                        true,
                                        false)
                                .toString())
                        .getFirstResponse()
                        .orElseThrow();
                upload(icon, mapped(server, iconUpload.getMap()));
                eventually(
                        () -> api.files().icons().stream()
                                .anyMatch(row -> row.getIconId().equals(Optional.of(iconId))
                                        && row.getFileSize() == icon.length),
                        "Uploaded icon must become complete");
                var icons = api.files().icons().stream()
                        .filter(row -> row.getIconId().equals(Optional.of(iconId)))
                        .toList();
                assertEquals(1, icons.size());
                assertEquals(icon.length, icons.getFirst().getFileSize());
                var iconDownload = api.executeRawCommand(FileCommands.ftInitDownload(
                                        5, FileLocation.ofServer(iconId.path()).withChannelPassword(""), 0)
                                .toString())
                        .getFirstResponse()
                        .orElseThrow();
                assertArrayEquals(icon, download(mapped(server, iconDownload.getMap())));
                api.files().deleteIcons(iconId);
                assertTrue(api.files().icons().stream()
                        .noneMatch(row -> row.getIconId().equals(Optional.of(iconId))));
                api.files()
                        .delete(java.util.Arrays.stream(new String[] {name, second})
                                .map(path -> FileLocation.of(cid, path))
                                .toList());
                api.files().delete(FileLocation.of(cid, "/contents"));
                assertTrue(api.files().list(FileLocation.of(cid, "/")).isEmpty());
                api.channels().delete(cid, true);
                query.exit();
                evidence.add(
                        "actual upload/download bytes, positive/bulk metadata, pending transfer/stop and file deletion passed");
            }
        } finally {
            var path = Path.of("build", "compatibility", target + "-file-contents.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, evidence);
        }
    }

    private static void upload(byte[] bytes, FileTransferParameters parameters) throws Exception {
        assertTrue(bytes.length <= 512, "This command probe uses only small payloads");
        try (var socket = dataSocket(parameters)) {
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
        }
    }

    private static byte[] download(FileTransferParameters parameters) throws Exception {
        int size = Math.toIntExact(parameters.getFileSize());
        assertTrue(size >= 0 && size <= 512);
        try (var socket = dataSocket(parameters)) {
            byte[] bytes = new byte[size];
            int offset = 0;
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (offset < size) {
                long remaining = deadline - System.nanoTime();
                assertTrue(remaining > 0, "Download must finish before deadline");
                socket.setSoTimeout((int) Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining)));
                int count = socket.getInputStream().read(bytes, offset, size - offset);
                assertTrue(count > 0, "Download must contain the announced bytes");
                offset += count;
            }
            return bytes;
        }
    }

    private static java.net.Socket dataSocket(FileTransferParameters parameters) throws Exception {
        var socket = new java.net.Socket();
        try {
            socket.connect(
                    new java.net.InetSocketAddress(
                            parameters.getFileServerHost().orElseThrow(), parameters.getFileServerPort()),
                    5000);
            socket.setSoTimeout(5000);
            socket.getOutputStream()
                    .write(parameters.getFileTransferKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            return socket;
        } catch (Throwable failure) {
            socket.close();
            throw failure;
        }
    }

    // Explicit fixture endpoint adaptation: public API NAT/endpoint behavior remains #442 acceptance.
    private static FileTransferParameters mapped(TeamSpeakContainer server, Map<String, String> response) {
        var values = new HashMap<>(response);
        values.put("ip", server.getHost());
        values.put("port", Integer.toString(server.getMappedPort(30033)));
        return new FileTransferParameters(values);
    }
}
