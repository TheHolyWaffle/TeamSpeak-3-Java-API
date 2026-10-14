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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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
        return requireInt("clientftfid");
    }

    /**
     * Returns an internal ID that the server uses to identify their file transfer.
     * This is <strong>not</strong> the key that can be used to request a file from the file server.
     *
     * @return the server's file transfer ID
     */
    public int getServerTransferId() {
        return requireInt("serverftfid");
    }

    /**
     * Gets the key needed identify ourselves to the file server and to start the file transfer.
     *
     * @return the file transfer key
     */
    public String getFileTransferKey() {
        return require("ftkey");
    }

    /**
     * Gets the IP address or hostname of the file server the TS3 instance wants us to send our data to.
     *
     * @return the IP address of the file server, or empty if the server did not send one
     */
    public Optional<String> getFileServerHost() {
        return findText("ip");
    }

    /**
     * Gets the port of the file server the TS3 instance wants us to send our data to.
     *
     * @return the port of the file server
     */
    public int getFileServerPort() {
        return requireInt("port");
    }

    /**
     * Gets the size of the file being downloaded in bytes.
     * Only present if this is a successfully started download.
     *
     * @return the size of the file being downloaded.
     */
    public long getFileSize() {
        return requireLong("size");
    }

    /**
     * Gets the failure the server reported inside an otherwise successful response,
     * built from {@link #getStatus()} and {@link #getMessage()}.
     *
     * @return the failure, or empty if the response carries no status or a successful one
     * @see QueryError#isSuccessful(int)
     */
    public Optional<QueryError> findFailure() {
        OptionalInt status = getStatus();
        if (status.isEmpty() || QueryError.isSuccessful(status.getAsInt())) return Optional.empty();
        Map<String, String> error = new LinkedHashMap<>();
        error.put("id", Integer.toString(status.getAsInt()));
        getMessage().ifPresent(message -> error.put("msg", message));
        return Optional.of(new QueryError(error));
    }

    /**
     * Gets the position from which a resumed upload continues.
     *
     * @return the number of bytes the server already has
     */
    public long getSeekPosition() {
        return requireLong("seekpos");
    }

    /**
     * Gets the status / error code of this command. Only present if the command failed.
     *
     * @return the command's status value, or empty if none was reported
     */
    public OptionalInt getStatus() {
        return findInt("status");
    }

    /**
     * Gets the message that is provided in case the command fails.
     *
     * @return the failure message, or empty if none was reported
     */
    public Optional<String> getMessage() {
        return find("msg");
    }
}
