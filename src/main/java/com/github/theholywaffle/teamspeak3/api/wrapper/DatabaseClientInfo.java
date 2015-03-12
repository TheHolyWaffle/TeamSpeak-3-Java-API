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

import java.util.Date;
import java.util.Map;

import com.github.theholywaffle.teamspeak3.api.ClientProperty;

public class DatabaseClientInfo extends Wrapper {

	public DatabaseClientInfo(Map<String, String> map) {
		super(map);
	}

	public String getUniqueIdentifier() {
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}

	public String getNickname() {
		return get(ClientProperty.CLIENT_NICKNAME);
	}

	public int getDatabaseId() {
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}

	public Date getCreated() {
		return new Date(getInt(ClientProperty.CLIENT_CREATED) * 1000);
	}

	public Date getLastConnected() {
		return new Date(getInt(ClientProperty.CLIENT_LASTCONNECTED) * 1000);
	}

	public int getTotalConnections() {
		return getInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
	}

	public String getAvatar() {
		return get(ClientProperty.CLIENT_FLAG_AVATAR);
	}

	public String getDescription() {
		return get(ClientProperty.CLIENT_DESCRIPTION);
	}

	public long getMonthlyBytesUploaded() {
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_UPLOADED);
	}

	public long getMonthlyBytesDownloaded() {
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_DOWNLOADED);
	}

	public long getTotalBytesUploaded() {
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_UPLOADED);
	}

	public long getTotalBytseDownloaded() {
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_DOWNLOADED);
	}

	public long getIconId() {
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}

	public String getBase64HashClientUID() {
		return get("client_base64HashClientUID");
	}

	public String getLastIp() {
		return get("client_lastip");
	}

}
