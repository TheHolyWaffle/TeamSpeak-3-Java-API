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

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class HostInfo extends Wrapper {

	public HostInfo(Map<String, String> map) {
		super(map);
	}

	public Duration getUptime() {
		return requireSeconds(ServerInstanceProperty.INSTANCE_UPTIME);
	}

	public Instant getTimeStamp() {
		return requireInstant(ServerInstanceProperty.HOST_TIMESTAMP_UTC);
	}

	public int getTotalRunningServers() {
		return requireInt(ServerInstanceProperty.VIRTUALSERVERS_RUNNING_TOTAL);
	}

	public int getTotalMaxClients() {
		return requireInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_MAXCLIENTS);
	}

	public int getTotalClientsOnline() {
		return requireInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_CLIENTS_ONLINE);
	}

	public int getTotalChannels() {
		return requireInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_CHANNELS_ONLINE);
	}

	public long getFileTransferBandwidthSent() {
		return requireLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
	}

	public long getFileTransferBandwidthReceived() {
		return requireLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
	}

	public long getFileTransferBytesSent() {
		return requireLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
	}

	public long getFileTransferBytesReceived() {
		return requireLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
	}

	public long getPacketsSentTotal() {
		return requireLong(ServerInstanceProperty.CONNECTION_PACKETS_SENT_TOTAL);
	}

	public long getBytesSentTotal() {
		return requireLong(ServerInstanceProperty.CONNECTION_BYTES_SENT_TOTAL);
	}

	public long getPacketsReceivedTotal() {
		return requireLong(ServerInstanceProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
	}

	public long getBytesReceivedTotal() {
		return requireLong(ServerInstanceProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
	}

	public long getBandwidthSentLastSecond() {
		return requireLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
	}

	public long getBandwidthSentLastMinute() {
		return requireLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthReceivedLastSecond() {
		return requireLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
	}

	public long getBandwidthReceivedLastMinute() {
		return requireLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
	}

}
