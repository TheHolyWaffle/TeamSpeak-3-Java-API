package com.github.theholywaffle.teamspeak3.api.wrapper;

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

import java.util.Map;

public class FileTransfer extends Wrapper {

	public FileTransfer(Map<String, String> map) {
		super(map);
	}

	/**
	 * Gets the ID of the client who started this file transfer.
	 *
	 * @return the ID of the involved client
	 */
	public int getClientId() {
		return getInt("clid");
	}

	/**
	 * Gets the path of the folder that the file which is being transferred is stored in on disk.
	 * This path seems to be relative to the folder the TS3 server is installed in.
	 *
	 * @return the disk path of the folder containing the transferring file
	 */
	public String getDiskFilePath() {
		return get("path");
	}

	/**
	 * Returns the name of the file on the TS3 server without its full path.
	 *
	 * @return the file's name
	 */
	public String getFileName() {
		return get("name");
	}

	/**
	 * Gets the size in bytes the file will have once it is fully transferred.
	 *
	 * @return the final file size in bytes
	 */
	public long getTotalFileSize() {
		return getLong("size");
	}

	/**
	 * Gets the amount of bytes that have already been transferred.
	 *
	 * @return the amount of transferred bytes
	 */
	public long getTransferredFileSize() {
		return getLong("sizedone");
	}

	/**
	 * Returns the key that the client has sent to the server to identify their file transfer.
	 *
	 * @return the client's file transfer ID
	 */
	public int getClientTransferId() {
		return getInt("clientftfid");
	}

	/**
	 * Returns an internal ID that the server uses to identify their file transfer.
	 * This is <strong>not</strong> the key that can be used to request a file from the file server.
	 *
	 * @return the server's file transfer ID
	 */
	public int getServerTransferId() {
		return getInt("serverftfid");
	}

	/**
	 * Returns the current status of the file transfer.
	 * <p>
	 * Currently known status codes:
	 * </p>
	 * <ul>
	 * <li>0 - Not started</li>
	 * <li>1 - Active</li>
	 * <li>2 - Inactive (paused, stalled or done)</li>
	 * </ul>
	 *
	 * @return the current transfer status
	 */
	public int getStatus() {
		return getInt("status");
	}

	/**
	 * Returns {@code true} if the file transfer has started.
	 *
	 * @return whether the file transfer has started
	 */
	public boolean hasStarted() {
		return getStatus() > 0;
	}

	/**
	 * Returns {@code true} if the file transfer is still active.
	 * This is the case when the transfer has started and is not done or paused.
	 *
	 * @return whether the file transfer is active
	 */
	public boolean isActive() {
		return getStatus() == 1;
	}

	/**
	 * Returns the current file transfer speed in bytes per second.
	 *
	 * @return the current transfer speed
	 */
	public double getCurrentSpeed() {
		return getDouble("current_speed");
	}

	/**
	 * Returns the current file transfer speed in bytes per second.
	 *
	 * @return the current transfer speed
	 */
	public double getAverageSpeed() {
		return getDouble("average_speed");
	}

	/**
	 * Returns how many milliseconds have elapsed since the file transfer was first announced to
	 * the server. This value will count up even if the file transfer has not yet been started,
	 * but will stop if the transfer has been paused or is complete.
	 *
	 * @return the transfer runtime in milliseconds
	 */
	public long getRuntime() {
		return getLong("runtime");
	}
}
