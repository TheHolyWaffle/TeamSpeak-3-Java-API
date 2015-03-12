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

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;

public class ConnectionInfo extends Wrapper {

	public ConnectionInfo(Map<String, String> map) {
		super(map);
	}

	public long getFiletransferBandwidthSent() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
	}

	public long getFiletransferBandwidthReceived() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
	}

	public long getFiletransferBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
	}

	public long getFiletransferBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
	}

	public long getTotalPacketsSent() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_TOTAL);
	}

	public long getTotalBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_SENT_TOTAL);
	}

	public long getTotalPacketsReceived() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
	}

	public long getTotalBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
	}

	public long getBandwidthSentLastSecond() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
	}

	public long getBandwidthSentLastMinute() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthReceivedLastSecond() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
	}

	public long getBandwidthReceivedLastMinute() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
	}

	public long getConnectedTime() {
		return getLong("connection_connected_time");
	}

	public double getPacketLoss() {
		return getDouble("connection_packetloss_total");
	}

	public double getPing() {
		return getDouble("connection_ping");
	}

}
