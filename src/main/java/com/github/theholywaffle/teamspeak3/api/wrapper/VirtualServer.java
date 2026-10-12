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

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class VirtualServer extends Wrapper {

	public VirtualServer(Map<String, String> map) {
		super(map);
	}

	public int getId() {
		return requireInt(VirtualServerProperty.VIRTUALSERVER_ID);
	}

	public int getPort() {
		return requireInt(VirtualServerProperty.VIRTUALSERVER_PORT);
	}

	/** @return the status, or {@link VirtualServerStatus#UNKNOWN} for a status this library does not know */
	public VirtualServerStatus getStatus() {
		return VirtualServerStatus.fromName(require(VirtualServerProperty.VIRTUALSERVER_STATUS));
	}

	/** @return the number of connected clients, or empty for a server that is not running */
	public OptionalInt getClientsOnline() {
		return findInt(VirtualServerProperty.VIRTUALSERVER_CLIENTSONLINE);
	}

	/** @return the number of connected query clients, or empty for a server that is not running */
	public OptionalInt getQueryClientsOnline() {
		return findInt(VirtualServerProperty.VIRTUALSERVER_QUERYCLIENTSONLINE);
	}

	/** @return the client limit, or empty for a server that is not running */
	public OptionalInt getMaxClients() {
		return findInt(VirtualServerProperty.VIRTUALSERVER_MAXCLIENTS);
	}

	public String getUniqueIdentifier() {
		return require(VirtualServerProperty.VIRTUALSERVER_UNIQUE_IDENTIFIER);
	}

	/** @return the uptime, or empty for a server that is not running */
	public Optional<Duration> getUptime() {
		return findLong(VirtualServerProperty.VIRTUALSERVER_UPTIME).stream().mapToObj(Duration::ofSeconds).findFirst();
	}

	public String getName() {
		return require(VirtualServerProperty.VIRTUALSERVER_NAME);
	}

	public boolean isAutoStart() {
		return requireBoolean(VirtualServerProperty.VIRTUALSERVER_AUTOSTART);
	}
}
