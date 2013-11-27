package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;

public class HostInfo extends Wrapper {

	public HostInfo(HashMap<String, String> map) {
		super(map);
	}

	public long getUptime() {
		return getLong(ServerInstanceProperty.INSTANCE_UPTIME);
	}

	public Date getTimeStamp() {
		return new Date(
				getLong(ServerInstanceProperty.HOST_TIMESTAMP_UTC)*1000);
	}

	public int getTotalRunningServers() {
		return getInt(ServerInstanceProperty.VIRTUALSERVERS_RUNNING_TOTAL);
	}

	public int getTotalMaxClients() {
		return getInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_MAXCLIENTS);
	}

	public int getTotalClientsOnline() {
		return getInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_CLIENTS_ONLINE);
	}

	public int getTotalChannels() {
		return getInt(ServerInstanceProperty.VIRTUALSERVERS_TOTAL_CHANNELS_ONLINE);
	}

	public long getFileTransferBandwidthSent() {
		return getLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
	}

	public long getFileTransferBandwidthReceived() {
		return getLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
	}

	public long getFileTransferBytesSent() {
		return getLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
	}

	public long getFileTransferBytesReceived() {
		return getLong(ServerInstanceProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
	}

	public long getPacketsSentTotal() {
		return getLong(ServerInstanceProperty.CONNECTION_PACKETS_SENT_TOTAL);
	}

	public long getBytesSentTotal() {
		return getLong(ServerInstanceProperty.CONNECTION_BYTES_SENT_TOTAL);
	}

	public long getPacketsReceivedTotal() {
		return getLong(ServerInstanceProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
	}

	public long getBytesReceivedTotal() {
		return getLong(ServerInstanceProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
	}

	public long getBandwidthSentLastSecond() {
		return getLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
	}

	public long getBandwidthSentLastMinute() {
		return getLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthReceivedLastSecond() {
		return getLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
	}

	public long getBandwidthReceivedLastMinute() {
		return getLong(ServerInstanceProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
	}

}
