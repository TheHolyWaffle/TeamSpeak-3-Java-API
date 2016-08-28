/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * A server query program which illustrates transferring files and icons.
 */
public class FileTransferExample {

	private static final String EXAMPLE_DIRECTORY = "example-directory";
	private static final String EXAMPLE_FILE_NAME = EXAMPLE_DIRECTORY + "/Hello World.txt";
	private static final byte[] EXAMPLE_FILE_CONTENT = "Hello file transferring world!".getBytes(StandardCharsets.UTF_8);

	private static final byte[] RED_EXAMPLE_ICON;
	private static final byte[] BLUE_EXAMPLE_ICON;

	static {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.createGraphics();

		g.setColor(Color.RED);
		g.fillRect(0, 0, 16, 16);
		ByteArrayOutputStream red = new ByteArrayOutputStream(128);
		try {
			ImageIO.write(img, "PNG", red);
		} catch (IOException e) {
			throw new IOError(e);
		}
		RED_EXAMPLE_ICON = red.toByteArray();

		g.setColor(Color.BLUE);
		g.fillRect(0, 0, 16, 16);
		ByteArrayOutputStream blue = new ByteArrayOutputStream(128);
		try {
			ImageIO.write(img, "PNG", blue);
		} catch (IOException e) {
			throw new IOError(e);
		}
		BLUE_EXAMPLE_ICON = blue.toByteArray();
	}

	public static void main(String[] args) {
		final TS3Config config = new TS3Config();
		config.setHost("77.77.77.77");
		config.setDebugLevel(Level.ALL);

		final TS3Query query = new TS3Query(config);
		query.connect();

		final TS3Api api = query.getApi();
		api.login("serveradmin", "serveradminpassword");
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		api.sendChannelMessage("PutPutBot is online!");

		// Set up properties for our test channels
		final HashMap<ChannelProperty, String> properties = new HashMap<>();
		properties.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1"); // Stay until restart
		int defaultChannelId = api.whoAmI().getChannelId();
		properties.put(ChannelProperty.CPID, String.valueOf(defaultChannelId));
		properties.put(ChannelProperty.CHANNEL_TOPIC, "File transfer tests");

		// --------------------- //
		// Direct file transfers //
		// --------------------- //

		// Create a new channel
		int directChannel = api.createChannel("Direct file transfers", properties);
		// Upload and set an icon
		long blackIconId = api.uploadIconDirect(RED_EXAMPLE_ICON);
		api.editChannel(directChannel, mapOf(ChannelProperty.CHANNEL_ICON_ID, blackIconId));
		// Create a new directory on the file repository
		api.createFileDirectory(EXAMPLE_DIRECTORY, directChannel);
		// And upload a file to it
		api.uploadFileDirect(EXAMPLE_FILE_CONTENT, EXAMPLE_FILE_NAME, false, directChannel);
		// Download it again and print it to System.out
		byte[] directDownload = api.downloadFileDirect(EXAMPLE_FILE_NAME, directChannel);
		System.out.println(new String(directDownload, StandardCharsets.UTF_8));

		// --------------------- //
		// Stream file transfers //
		// --------------------- //

		// Create a new channel
		int streamChannel = api.createChannel("Stream file transfers", properties);
		// Upload and set an icon
		InputStream iconIn = new ByteArrayInputStream(BLUE_EXAMPLE_ICON); // Usually a FileInputStream
		long whiteIconId = api.uploadIcon(iconIn, BLUE_EXAMPLE_ICON.length);
		api.editChannel(streamChannel, mapOf(ChannelProperty.CHANNEL_ICON_ID, whiteIconId));
		// Create a new directory on the file repository
		api.createFileDirectory(EXAMPLE_DIRECTORY, streamChannel);
		// And upload a file to it
		InputStream dataIn = new ByteArrayInputStream(EXAMPLE_FILE_CONTENT); // Usually a FileInputStream
		api.uploadFile(dataIn, EXAMPLE_FILE_CONTENT.length, EXAMPLE_FILE_NAME, false, streamChannel);
		// Download it again and print it to System.out
		ByteArrayOutputStream dataOut = new ByteArrayOutputStream(128); // Usually a FileOutputStream
		api.downloadFile(dataOut, EXAMPLE_FILE_NAME, streamChannel);
		System.out.println(new String(dataOut.toByteArray(), StandardCharsets.UTF_8));

		// We're done, disconnect
		query.exit();
	}

	private static Map<ChannelProperty, String> mapOf(ChannelProperty property, Object value) {
		return Collections.singletonMap(property, String.valueOf(value));
	}
}
