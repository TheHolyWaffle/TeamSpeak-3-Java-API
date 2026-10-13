package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.UploadMode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Uploads and downloads a file and an icon, once with byte arrays and once with streams, in a
 * temporary channel that is deleted afterwards (together with its files).
 * <p>
 * File transfers use a separate, unencrypted TCP connection to the server's file transfer port
 * (30033 by default), even when the query itself uses SSH. Set {@code TS3_FILE_TRANSFER_HOST} and
 * {@code TS3_FILE_TRANSFER_PORT} when the address the server advertises is not reachable, for example
 * behind NAT or Docker port mapping.
 * </p>
 */
public final class FileTransferExample {

    private static final String DIRECTORY = "/example-directory";
    private static final String FILE_NAME = DIRECTORY + "/Hello World.txt";
    private static final byte[] CONTENT = "Hello file transferring world!".getBytes(StandardCharsets.UTF_8);

    private FileTransferExample() {}

    public static void main(String[] args) {
        // Icons are drawn off-screen; without this, AWT can keep the JVM alive on macOS.
        System.setProperty("java.awt.headless", "true");
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3Query query = new TS3Query(settings.config())) {
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());

            // Semi-permanent: the channel would also disappear when the server restarts.
            int channelId = api.channels()
                    .create("Java API file transfer example", Map.of(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1"));
            try {
                FileLocation file = FileLocation.of(channelId, FILE_NAME);
                api.files().createDirectory(FileLocation.of(channelId, DIRECTORY));

                // Byte arrays: simplest, but downloads are limited by FileTransferConfig.maxInMemoryBytes().
                long redIcon = api.files().uploadIcon(icon(Color.RED));
                api.files().upload(file, CONTENT, UploadMode.CREATE);
                byte[] downloaded = api.files().downloadBytes(file);
                System.out.println("Byte array download: " + new String(downloaded, StandardCharsets.UTF_8));

                // Streams: use files instead of in-memory streams for large data.
                // The library never closes or flushes streams owned by the caller.
                byte[] blue = icon(Color.BLUE);
                long blueIcon;
                try (InputStream iconIn = new ByteArrayInputStream(blue);
                        InputStream dataIn = new ByteArrayInputStream(CONTENT)) {
                    blueIcon = api.files().uploadIcon(iconIn, blue.length);
                    api.files().upload(file, dataIn, CONTENT.length, UploadMode.OVERWRITE);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
                long length = api.files().download(file, dataOut);
                System.out.println(
                        "Stream download (" + length + " bytes): " + dataOut.toString(StandardCharsets.UTF_8));

                // TS3 assigns channel icons through the i_icon_id permission, using the ID's signed 32-bit form.
                api.permissions().add(PermissionTarget.channel(channelId), "i_icon_id", (int) blueIcon);
                System.out.println("Uploaded icons " + redIcon + " and " + blueIcon + "; channel icon is now "
                        + api.channels().info(channelId).getIconId());
                api.files().deleteIcons(redIcon, blueIcon);
            } finally {
                api.channels().delete(channelId, true);
            }

            query.exit();
        }
    }

    private static byte[] icon(Color color) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, 16, 16);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return png.toByteArray();
    }
}
