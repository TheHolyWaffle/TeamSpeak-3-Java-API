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

import java.util.Map;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.VirtualServerStatus;

public class ServerQueryInfo extends Wrapper {

	public ServerQueryInfo(Map<String, String> map) {
		super(map);
	}

	public int getChannelId() {
		return getInt("client_channel_id");
	}

	public int getDatabaseId() {
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}

	public int getId() {
		return getInt("client_id");
	}

	public String getLoginName() {
		return get(ClientProperty.CLIENT_LOGIN_NAME);
	}

	public String getNickname() {
		return get(ClientProperty.CLIENT_NICKNAME);
	}

	public int getOriginServerId() {
		return getInt("client_origin_server_id");
	}

	public String getUniqueIdentifier() {
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}

	public int getVirtualServerId() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_ID);
	}

	public int getVirtualServerPort() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_PORT);
	}

	public VirtualServerStatus getVirtualServerStatus() {
		final String status = get(VirtualServerProperty.VIRTUALSERVER_STATUS);
		for (final VirtualServerStatus s : VirtualServerStatus.values()) {
			if (s.getName().equals(status)) {
				return s;
			}
		}
		return VirtualServerStatus.UNKNOWN;
	}

	public String getVirtualServerUniqueIdentifier() {
		return get(VirtualServerProperty.VIRTUALSERVER_UNIQUE_IDENTIFIER);
	}
}
