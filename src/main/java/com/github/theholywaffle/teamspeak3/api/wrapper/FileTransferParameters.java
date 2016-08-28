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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an internally started file transfer and encapsulates the command response.
 */
public class FileTransferParameters extends Wrapper {

	public FileTransferParameters(Map<String, String> map) {
		super(map);
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
	 * Gets the key needed identify ourselves to the file server and to start the file transfer.
	 *
	 * @return the file transfer key
	 */
	public String getFileTransferKey() {
		return get("ftkey");
	}

	/**
	 * Gets the IP address or hostname of the file server the TS3 instance wants us to send our data to.
	 *
	 * @return the IP address of the file server
	 */
	public String getFileServerHost() {
		return get("ip");
	}

	/**
	 * Gets the port of the file server the TS3 instance wants us to send our data to.
	 *
	 * @return the port of the file server
	 */
	public int getFileServerPort() {
		return getInt("port");
	}

	/**
	 * Gets the size of the file being downloaded in bytes.
	 * Only present if this is a successfully started download.
	 *
	 * @return the size of the file being downloaded.
	 */
	public long getFileSize() {
		return getLong("size");
	}

	/**
	 * Gets a {@link QueryError} that can be used when throwing an exception.
	 *
	 * @return a query error for this command's success value
	 */
	public QueryError getQueryError() {
		Map<String, String> errorMap = new HashMap<>(2);
		errorMap.put("id", String.valueOf(getStatus()));
		errorMap.put("msg", getMessage());
		return new QueryError(errorMap);
	}

	/**
	 * Gets the status / error code of this command. Only present if the command failed.
	 *
	 * @return the command's status value
	 */
	public int getStatus() {
		final int status = getInt("status");
		return status == -1 ? 0 : status;
	}

	/**
	 * Gets the message that is provided in case the command fails.
	 *
	 * @return the failure message
	 */
	public String getMessage() {
		return get("msg");
	}
}
