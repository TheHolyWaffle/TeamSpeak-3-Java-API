package com.github.theholywaffle.teamspeak3.api.event;

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

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;

import java.util.Map;

public class PrivilegeKeyUsedEvent extends BaseEvent {

	public PrivilegeKeyUsedEvent(Map<String, String> map) {
		super(map);
	}

	public int getClientId() {
		return getInt("clid");
	}

	public int getClientDatabaseId() {
		return getInt("cldbid");
	}

	public String getClientUniqueIdentifier() {
		return get("cluid");
	}

	public String getPrivilegeKey() {
		return get("token");
	}

	public PrivilegeKeyType getPrivilegeKeyType() {
		if (getPrivilegeKeyChannelId() == 0) {
			return PrivilegeKeyType.SERVER_GROUP;
		} else {
			return PrivilegeKeyType.CHANNEL_GROUP;
		}
	}

	public int getPrivilegeKeyGroupId() {
		return getInt("token1");
	}

	public int getPrivilegeKeyChannelId() {
		return getInt("token2");
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onPrivilegeKeyUsed(this);
	}
}
