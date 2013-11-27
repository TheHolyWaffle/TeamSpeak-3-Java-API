package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;

public class ConnectionInfo extends Wrapper {

	public ConnectionInfo(HashMap<String, String> map) {
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
