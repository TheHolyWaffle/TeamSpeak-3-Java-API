package com.github.theholywaffle.teamspeak3.api.wrapper;

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

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class DatabaseClient extends Wrapper {

    public DatabaseClient(Map<String, String> map) {
        super(map);
    }

    public ClientDatabaseId getDatabaseId() {
        return requireDatabaseId("cldbid");
    }

    public String getUniqueIdentifier() {
        return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
    }

    public String getNickname() {
        return require(ClientProperty.CLIENT_NICKNAME);
    }

    /** @return when the identity was first seen, or empty if not recorded */
    public Optional<Instant> getCreatedDate() {
        return requireTimestamp(ClientProperty.CLIENT_CREATED);
    }

    /** @return when the identity last connected, or empty if not recorded */
    public Optional<Instant> getLastConnectedDate() {
        return requireTimestamp(ClientProperty.CLIENT_LASTCONNECTED);
    }

    public int getTotalConnections() {
        return requireInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
    }

    public String getDescription() {
        return require(ClientProperty.CLIENT_DESCRIPTION);
    }

    /** @return the last IP address, or empty if none is recorded */
    public Optional<String> getLastIp() {
        return findText("client_lastip");
    }
}
