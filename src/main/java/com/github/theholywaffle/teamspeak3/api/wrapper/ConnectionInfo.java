package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import java.time.Duration;
import java.util.Map;

public class ConnectionInfo extends Wrapper {

    public ConnectionInfo(Map<String, String> map) {
        super(map);
    }

    public long getFiletransferBandwidthSent() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
    }

    public long getFiletransferBandwidthReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
    }

    public long getFiletransferBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
    }

    public long getFiletransferBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
    }

    public long getTotalPacketsSent() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_TOTAL);
    }

    public long getTotalBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_SENT_TOTAL);
    }

    public long getTotalPacketsReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
    }

    public long getTotalBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
    }

    public long getBandwidthSentLastSecond() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
    }

    public long getBandwidthSentLastMinute() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
    }

    public long getBandwidthReceivedLastSecond() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
    }

    public long getBandwidthReceivedLastMinute() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
    }

    /** @return the virtual server's uptime as reported by this command */
    public Duration getConnectedTime() {
        return requireSeconds("connection_connected_time");
    }

    public double getPacketLoss() {
        return requireDouble("connection_packetloss_total");
    }

    public double getPing() {
        return requireDouble("connection_ping");
    }
}
