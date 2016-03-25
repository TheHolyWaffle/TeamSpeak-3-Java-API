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

import com.github.theholywaffle.teamspeak3.api.ClientProperty;

import java.util.Map;

public class ClientInfo extends Client {

	private final int clientId;

	public ClientInfo(int clientId, Map<String, String> map) {
		super(map);
		this.clientId = clientId;
	}

	@Override
	public int getId() {
		return clientId;
	}

	public String getAvatar() {
		return get(ClientProperty.CLIENT_FLAG_AVATAR);
	}

	public long getBandwidthReceivedLastMinute() {
		return getLong(ClientProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthReceivedLastSecond() {
		return getLong(ClientProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
	}

	public long getBandwidthSentlastMinute() {
		return getLong(ClientProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthSentLastSecond() {
		return getLong(ClientProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
	}

	public String getBase64ClientUId() {
		return get("client_base64HashClientUID");
	}

	public int getDefaultChannel() {
		// TeamSpeak decided to prefix the channel ID with a forward slash (/)...
		final String channelId = get(ClientProperty.CLIENT_DEFAULT_CHANNEL);
		if (channelId.isEmpty()) return -1;
		return Integer.parseInt(channelId.substring(1));
	}

	public String getDefaultToken() {
		return get(ClientProperty.CLIENT_DEFAULT_TOKEN);
	}

	public String getDescription() {
		return get(ClientProperty.CLIENT_DESCRIPTION);
	}

	public long getFiletransferBandwidthReceived() {
		return getLong(ClientProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
	}

	public long getFiletransferBandwidthSent() {
		return getLong(ClientProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
	}

	public String getLoginName() {
		return get(ClientProperty.CLIENT_LOGIN_NAME);
	}

	public String getMetaData() {
		return get(ClientProperty.CLIENT_META_DATA);
	}

	public long getMonthlyBytesDownloaded() {
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_DOWNLOADED);
	}

	public long getMonthlyBytesUploaded() {
		return getLong(ClientProperty.CLIENT_MONTH_BYTES_UPLOADED);
	}

	public int getNeededServerQueryViewPower() {
		return getInt(ClientProperty.CLIENT_NEEDED_SERVERQUERY_VIEW_POWER);
	}

	public String getPhoneticNickname() {
		return get(ClientProperty.CLIENT_NICKNAME_PHONETIC);
	}

	public String getTalkRequestMessage() {
		return get(ClientProperty.CLIENT_TALK_REQUEST_MSG);
	}

	public long getTimeConnected() { // milliseconds
		return getLong(ClientProperty.CONNECTION_CONNECTED_TIME);
	}

	public long getTotalBytesDownloaded() {
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_DOWNLOADED);
	}

	public long getTotalBytesReceived() {
		return getLong(ClientProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
	}

	public long getTotalBytesSent() {
		return getLong(ClientProperty.CONNECTION_BYTES_SENT_TOTAL);
	}

	public long getTotalBytesUploaded() {
		return getLong(ClientProperty.CLIENT_TOTAL_BYTES_UPLOADED);
	}

	public int getTotalConnections() {
		return getInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
	}

	public long getTotalPacketsReceived() {
		return getLong(ClientProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
	}

	public long getTotalPacketsSent() {
		return getLong(ClientProperty.CONNECTION_PACKETS_SENT_TOTAL);
	}

	public int getUnreadMessages() {
		return getInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
	}

	public boolean isOutputOnlyMuted() {
		return getBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
	}

	public boolean isRequestingToTalk() {
		return getBoolean(ClientProperty.CLIENT_TALK_REQUEST);
	}

	@Override
	public boolean isTalking() {
		throw new UnsupportedOperationException();
	}
}
