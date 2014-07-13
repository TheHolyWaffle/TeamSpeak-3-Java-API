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

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;

public class VirtualServer extends Wrapper {

	public VirtualServer(HashMap<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt("virtualserver_id");
	}

	public int getPort() {
		return getInt("virtualserver_port");
	}

	public VirtualServerStatus getStatus() {
		for (final VirtualServerStatus s : VirtualServerStatus.values()) {
			if (s.getName().equals(get("virtualserver_status"))) {
				return s;
			}
		}
		return VirtualServerStatus.UNKNOWN;
	}

	public int getClientsOnline() {
		return getInt("virtualserver_clientsonline");
	}

	public int getQueryClientsOnline() {
		return getInt("virtualserver_queryclientsonline");
	}

	public int getMaxClients() {
		return getInt("virtualserver_maxclients");
	}

	public int getUptime() {
		return getInt("virtualserver_uptime"); // in seconds
	}

	public String getName() {
		return get("virtualserver_name");
	}

	public boolean isAutoStart() {
		return getBoolean("virtualserver_autostart");
	}

}
