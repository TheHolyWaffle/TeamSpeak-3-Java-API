package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2016 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

public class FileTransferHelper {

	private static final Logger log = LoggerFactory.getLogger(FileTransferHelper.class);
	private static final int BUFFER_SIZE = 16_384; // 16 kB

	// Can only be in the range 0 - 65535
	private final AtomicInteger clientTransferId = new AtomicInteger(0);
	private final String defaultHost;

	FileTransferHelper(String host) {
		defaultHost = host;
	}

	// FILES

	public void downloadFile(OutputStream dataOut, FileTransferParameters params) throws IOException {
		final String host = getHostFromResponse(params.getFileServerHost());
		final int port = params.getFileServerPort();
		final long dataLength = params.getFileSize();
		final int downloadId = params.getClientTransferId() + 1;

		log.info("[Download {}] Download started", downloadId);
		try (Socket socket = new Socket(host, port)) {
			socket.setReceiveBufferSize(BUFFER_SIZE);
			int actualSize = socket.getReceiveBufferSize();

			OutputStream out = socket.getOutputStream();
			out.write(params.getFileTransferKey().getBytes("UTF-8"));
			out.flush();

			InputStream in = socket.getInputStream();
			byte[] buffer = new byte[actualSize];
			long total = 0;
			while (total < dataLength) {
				int read = in.read(buffer);
				if (read < 0) throw new IOException("Server response contained less data than specified");
				total += read;
				if (total > dataLength) throw new IOException("Server response contained more data than specified");
				dataOut.write(buffer, 0, read);
			}
			log.info("[Download {}] Download finished", downloadId);
		} catch (IOException e) {
			// Log and re-throw
			log.warn("[Download {}] Download failed: {}", downloadId, e.getMessage());
			throw e;
		}
	}

	public void uploadFile(InputStream dataIn, long dataLength, FileTransferParameters params) throws IOException {
		final String host = getHostFromResponse(params.getFileServerHost());
		final int port = params.getFileServerPort();
		final int uploadId = params.getClientTransferId() + 1;

		log.info("[Upload {}] Upload started", uploadId);
		try (Socket socket = new Socket(host, port)) {
			socket.setSendBufferSize(BUFFER_SIZE);
			int actualSize = socket.getSendBufferSize();

			OutputStream out = socket.getOutputStream();
			out.write(params.getFileTransferKey().getBytes("UTF-8"));
			out.flush();

			byte[] buffer = new byte[actualSize];
			long total = 0;
			while (total < dataLength) {
				int toRead = (int) Math.min(actualSize, dataLength - total);
				int read = dataIn.read(buffer, 0, toRead);
				if (read < 0) throw new IOException("User stream did not contain enough data");
				total += read;
				out.write(buffer, 0, read);
			}
			log.info("[Upload {}] Upload finished", uploadId);
		} catch (IOException e) {
			// Log and re-throw
			log.warn("[Upload {}] Upload failed: {}", uploadId, e.getMessage());
			throw e;
		}
	}

	// ICONS

	public long getIconId(byte[] data) {
		final CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}

	// UTIL

	public byte[] readFully(InputStream dataIn, long dataLength) throws IOException {
		if (dataLength > Integer.MAX_VALUE) throw new IOException("File too large");

		final int len = (int) dataLength;
		byte[] data = new byte[len];
		int total = 0;
		while (total < len) {
			int read = dataIn.read(data, total, len - total);
			if (read < 0) throw new IOException("User stream did not contain enough data");
			total += read;
		}
		return data;
	}

	public int getClientTransferId() {
		// AtomicInteger#getAndUpdate without Java 8
		int prev, next;
		do {
			prev = clientTransferId.get();
			next = (prev + 1) & 0xFFFF;
		} while (!clientTransferId.compareAndSet(prev, next));
		return prev;
	}

	private String getHostFromResponse(String raw) {
		if (raw == null || raw.isEmpty()) return defaultHost;
		if (raw.startsWith("0.0.0.0")) return defaultHost;
		int firstComma = raw.indexOf(',');
		if (firstComma <= 0) return defaultHost;
		return raw.substring(0, firstComma);
	}
}
