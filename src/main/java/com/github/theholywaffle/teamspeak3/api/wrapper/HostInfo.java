package com.github.theholywaffle.teamspeak3.api.wrapper;

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
