package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import com.github.theholywaffle.teamspeak3.commands.FileCommands;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class FileContentCompatibilityIT {
    @ParameterizedTest @EnumSource(ServerQueryCompatibilityIT.Target.class) @Timeout(120)
    void contentsAndTransferCommands(ServerQueryCompatibilityIT.Target target) throws Exception {
        var evidence = new ArrayList<String>();
        try (var server = new TeamSpeakContainer()) {
            server.addExposedPort(30033); server.start();
            try (var query = new TS3Query(server.config(target.protocol))) {
                query.connect(); var api = query.getApi(); api.selectVirtualServerById(1);
                for (String command : List.of("ftinitupload", "ftinitdownload", "ftlist", "ftstop", "ftgetfileinfo")) {
                    var help = api.executeRawCommand("help " + command).getRawResponse();
                    assertTrue(help.startsWith("Usage: " + command)); evidence.add("help " + command + "\n" + help);
                }
                int cid = api.createChannel("file-content-" + target, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
                byte[] bytes = "actual bytes 日本語 | literal \\s\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                api.createFileDirectory("/contents", cid);
                String name = "/contents/content 日本語.txt";
                var upload = api.executeRawCommand(FileCommands.ftInitUpload(1, name, cid, "", bytes.length, true).toString()).getFirstResponse();
                evidence.add("upload status=" + upload.get("status") + " message=" + upload.get("msg"));
                assertFalse(upload.get("ftkey").isBlank(), "Upload initialization failed: status=" + upload.get("status") + " message=" + upload.get("msg")); assertEquals(30033, upload.getInt("port"));
                evidence.add("upload fields=" + new java.util.TreeSet<>(upload.getMap().keySet()));
                upload(bytes, mapped(server, upload.getMap()));
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                while (api.getFileInfo(name, cid).getFileSize() != bytes.length) {
                    assertTrue(System.nanoTime() < deadline, "Uploaded size must become visible");
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(25);
                }
                var info = api.getFileInfo(name, cid); assertTrue(info.isFile()); assertEquals(name, info.getPath());
                String second = "/contents/second.bin";
                var secondUpload = api.executeRawCommand(FileCommands.ftInitUpload(6, second, cid, "", 1, true).toString()).getFirstResponse();
                upload(new byte[]{42}, mapped(server, secondUpload.getMap()));
                long secondDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                while (api.getFileInfo(second, cid).getFileSize() != 1) {
                    assertTrue(System.nanoTime() < secondDeadline, "Second uploaded file must become complete");
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(25);
                }
                var bulk = api.getFileInfos(new String[]{name, second}, new int[]{cid, cid}, new String[]{"", ""});
                assertEquals(2, bulk.size());
                var sizes = bulk.stream().collect(java.util.stream.Collectors.toMap(row -> row.getPath(), row -> row.getFileSize()));
                assertEquals(Map.of(name, (long) bytes.length, second, 1L), sizes);
                evidence.add("positive file-info fields=" + new java.util.TreeSet<>(info.getMap().keySet()));
                var download = api.executeRawCommand(FileCommands.ftInitDownload(2, name, cid, "").toString()).getFirstResponse();
                assertEquals(bytes.length, download.getLong("size"));
                assertArrayEquals(bytes, download(mapped(server, download.getMap())));
                var pending = api.executeRawCommand(FileCommands.ftInitUpload(3, "/pending.bin", cid, "", 100, true).toString()).getFirstResponse();
                int transfer = pending.getInt("serverftfid"); assertTrue(transfer >= 0);
                var listed = api.getFileTransfers().stream().filter(row -> row.getServerTransferId() == transfer).toList();
                assertEquals(1, listed.size()); assertEquals(3, listed.getFirst().getClientTransferId());
                assertEquals(api.whoAmI().getId(), listed.getFirst().getClientId());
                evidence.add("pending transfer fields=" + new java.util.TreeSet<>(listed.getFirst().getMap().keySet()));
                try (var socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(server.getHost(), server.getMappedPort(30033)), 5000);
                    socket.setSoTimeout(5000); socket.getOutputStream().write(pending.get("ftkey").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    long activeDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                    while (api.getFileTransfers().stream().noneMatch(row -> row.getServerTransferId() == transfer && row.isActive())) {
                        assertTrue(System.nanoTime() < activeDeadline, "Authenticated partial upload must become active");
                        java.util.concurrent.TimeUnit.MILLISECONDS.sleep(25);
                    }
                    api.executeRawCommand("ftstop serverftfid=" + transfer + " delete=1");
                    assertEquals(-1, socket.getInputStream().read(), "Stopped upload must close its data socket");
                }
                assertTrue(api.getFileTransfers().stream().noneMatch(row -> row.getServerTransferId() == transfer));
                byte[] icon = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXh8AAAAASUVORK5CYII=");
                long iconId = FileTransferHelper.getIconId(icon);
                var iconUpload = api.executeRawCommand(FileCommands.ftInitUpload(4, "/icon_" + iconId, 0, "", icon.length, true).toString()).getFirstResponse();
                upload(icon, mapped(server, iconUpload.getMap()));
                long iconDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                while (api.getIconList().stream().noneMatch(row -> row.getIconId() == iconId && row.getFileSize() == icon.length)) {
                    assertTrue(System.nanoTime() < iconDeadline, "Uploaded icon must become complete");
                    java.util.concurrent.TimeUnit.MILLISECONDS.sleep(25);
                }
                var icons = api.getIconList().stream().filter(row -> row.getIconId() == iconId).toList();
                assertEquals(1, icons.size()); assertEquals(icon.length, icons.getFirst().getFileSize());
                var iconDownload = api.executeRawCommand(FileCommands.ftInitDownload(5, "/icon_" + iconId, 0, "").toString()).getFirstResponse();
                assertArrayEquals(icon, download(mapped(server, iconDownload.getMap())));
                api.deleteIcons(iconId); assertTrue(api.getIconList().stream().noneMatch(row -> row.getIconId() == iconId));
                api.deleteFiles(new String[]{name, second}, cid); api.deleteFile("/contents", cid); assertTrue(api.getFileList("/", cid).isEmpty());
                api.deleteChannel(cid, true); query.exit();
                evidence.add("actual upload/download bytes, positive/bulk metadata, pending transfer/stop and file deletion passed");
            }
        } finally {
            var path = Path.of("target", "compatibility", target + "-file-contents.txt");
            Files.createDirectories(path.getParent()); Files.write(path, evidence);
        }
    }

    private static void upload(byte[] bytes, FileTransferParameters parameters) throws Exception {
        assertTrue(bytes.length <= 512, "This command probe uses only small payloads");
        try (var socket = dataSocket(parameters)) {
            socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
        }
    }

    private static byte[] download(FileTransferParameters parameters) throws Exception {
        int size = Math.toIntExact(parameters.getFileSize()); assertTrue(size >= 0 && size <= 512);
        try (var socket = dataSocket(parameters)) {
            byte[] bytes = new byte[size]; int offset = 0;
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (offset < size) {
                long remaining = deadline - System.nanoTime(); assertTrue(remaining > 0, "Download must finish before deadline");
                socket.setSoTimeout((int) Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining)));
                int count = socket.getInputStream().read(bytes, offset, size - offset);
                assertTrue(count > 0, "Download must contain the announced bytes"); offset += count;
            }
            return bytes;
        }
    }

    private static java.net.Socket dataSocket(FileTransferParameters parameters) throws Exception {
        var socket = new java.net.Socket();
        try {
            socket.connect(new java.net.InetSocketAddress(parameters.getFileServerHost(), parameters.getFileServerPort()), 5000);
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(parameters.getFileTransferKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            socket.getOutputStream().flush(); return socket;
        } catch (Throwable failure) { socket.close(); throw failure; }
    }

    // Explicit fixture endpoint adaptation: public API NAT/endpoint behavior remains #442 acceptance.
    private static FileTransferParameters mapped(TeamSpeakContainer server, Map<String, String> response) {
        var values = new HashMap<>(response);
        values.put("ip", server.getHost()); values.put("port", Integer.toString(server.getMappedPort(30033)));
        return new FileTransferParameters(values);
    }
}
