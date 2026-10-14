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

import com.github.theholywaffle.teamspeak3.api.CodecEncryptionMode;
import com.github.theholywaffle.teamspeak3.api.HostBannerMode;
import com.github.theholywaffle.teamspeak3.api.HostMessageMode;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class VirtualServerInfo extends VirtualServer {

    public VirtualServerInfo(Map<String, String> map) {
        super(map);
    }

    public int getAntifloodPointsNeededCommandBlock() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_NEEDED_COMMAND_BLOCK);
    }

    public int getAntifloodPointsNeedIpBlock() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_NEEDED_IP_BLOCK);
    }

    public int getAntifloodPointsTickReduce() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_TICK_REDUCE);
    }

    public long getBandwidthReceivedLastMinute() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
    }

    public long getBandwidthReceivedLastSecond() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
    }

    public long getBandwidthSentLastMinute() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
    }

    public long getBandwidthSentLastSecond() {
        return requireLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
    }

    public int getChannelsOnline() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_CHANNELSONLINE);
    }

    /** @return the encryption mode, or {@link CodecEncryptionMode#UNKNOWN} for a mode this library does not know */
    public CodecEncryptionMode getCodecEncryptionMode() {
        return requireCode(VirtualServerProperty.VIRTUALSERVER_CODEC_ENCRYPTION_MODE, CodecEncryptionMode::fromIndex);
    }

    public int getComplaintAutobanCount() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_AUTOBAN_COUNT);
    }

    public Duration getComplaintAutobanTime() {
        return requireSeconds(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_AUTOBAN_TIME);
    }

    public Duration getComplaintRemoveTime() {
        return requireSeconds(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_REMOVE_TIME);
    }

    public long getControlBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_CONTROL);
    }

    public long getControlBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_SENT_CONTROL);
    }

    public long getControlPacketsReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_CONTROL);
    }

    public long getControlPacketsSent() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_CONTROL);
    }

    public Instant getCreatedDate() {
        return requireInstant(VirtualServerProperty.VIRTUALSERVER_CREATED);
    }

    public int getDefaultChannelAdminGroup() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_CHANNEL_ADMIN_GROUP);
    }

    public int getDefaultChannelGroup() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_CHANNEL_GROUP);
    }

    public int getDefaultServerGroup() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_SERVER_GROUP);
    }

    /** @return the monthly download quota in MiB, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getDownloadQuota() {
        return requireLimit(VirtualServerProperty.VIRTUALSERVER_DOWNLOAD_QUOTA);
    }

    public String getFileBase() {
        return require(VirtualServerProperty.VIRTUALSERVER_FILEBASE);
    }

    public long getFiletransferBandwidthReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
    }

    public long getFiletransferBandwidthSent() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
    }

    public long getFiletransferBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
    }

    public long getFiletransferBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
    }

    public Duration getHostbannerGfxInterval() {
        return requireSeconds(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_GFX_INTERVAL);
    }

    public String getHostbannerGfxUrl() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_GFX_URL);
    }

    /** @return the host banner mode, or {@link HostBannerMode#UNKNOWN} for a mode this library does not know */
    public HostBannerMode getHostbannerMode() {
        return requireCode(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_MODE, HostBannerMode::fromIndex);
    }

    public String getHostbannerUrl() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_URL);
    }

    public String getHostbuttonGfxUrl() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_GFX_URL);
    }

    public String getHostbuttonTooltip() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_TOOLTIP);
    }

    public String getHostbuttonUrl() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_URL);
    }

    public String getHostMessage() {
        return require(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE);
    }

    /** @return the host message mode, or {@link HostMessageMode#UNKNOWN} for a mode this library does not know */
    public HostMessageMode getHostMessageMode() {
        return requireCode(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE_MODE, HostMessageMode::fromIndex);
    }

    public long getIconId() {
        return requireLong(VirtualServerProperty.VIRTUALSERVER_ICON_ID);
    }

    /** @return the configured bind addresses, comma separated, or empty if none are configured */
    public Optional<String> getIp() {
        return findText(VirtualServerProperty.VIRTUALSERVER_IP);
    }

    public long getKeepAliveBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_KEEPALIVE);
    }

    public long getKeepAliveBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_SENT_KEEPALIVE);
    }

    public long getKeepAlivePacketsReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_KEEPALIVE);
    }

    public long getKeepAlivePacketsSent() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_KEEPALIVE);
    }

    public String getMachineId() {
        return require(VirtualServerProperty.VIRTUALSERVER_MACHINE_ID);
    }

    /** @return the limit in bytes per second, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getMaxDownloadBandwidth() {
        return requireLimit(VirtualServerProperty.VIRTUALSERVER_MAX_DOWNLOAD_TOTAL_BANDWIDTH);
    }

    /** @return the limit in bytes per second, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getMaxUploadBandwidth() {
        return requireLimit(VirtualServerProperty.VIRTUALSERVER_MAX_UPLOAD_TOTAL_BANDWIDTH);
    }

    public int getMinClientsInChannelBeforeForcedSilence() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_MIN_CLIENTS_IN_CHANNEL_BEFORE_FORCED_SILENCE);
    }

    public int getMinimumClientVersion() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_MIN_CLIENT_VERSION);
    }

    public long getMonthlyBytesDownloaded() {
        return requireLong(VirtualServerProperty.VIRTUALSERVER_MONTH_BYTES_DOWNLOADED);
    }

    public long getMonthlyBytesUploaded() {
        return requireLong(VirtualServerProperty.VIRTUALSERVER_MONTH_BYTES_UPLOADED);
    }

    public int getNeededIdentitySecurityLevel() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_NEEDED_IDENTITY_SECURITY_LEVEL);
    }

    /** @return the hashed server password, or empty if none is set */
    public Optional<String> getPassword() {
        return findText(VirtualServerProperty.VIRTUALSERVER_PASSWORD);
    }

    public String getPhoneticName() {
        return require(VirtualServerProperty.VIRTUALSERVER_NAME_PHONETIC);
    }

    public double getPing() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PING);
    }

    public String getPlatform() {
        return require(VirtualServerProperty.VIRTUALSERVER_PLATFORM);
    }

    public double getPrioritySpeakerDimmModificator() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_PRIORITY_SPEAKER_DIMM_MODIFICATOR);
    }

    public int getReservedSlots() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_RESERVED_SLOTS);
    }

    public long getSpeechBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_SPEECH);
    }

    public long getSpeechBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_SENT_SPEECH);
    }

    public long getSpeechPacketsReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_SPEECH);
    }

    public long getSpeechPacketsSent() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_SPEECH);
    }

    public long getTotalBytesDownloaded() {
        return requireLong(VirtualServerProperty.VIRTUALSERVER_TOTAL_BYTES_DOWNLOADED);
    }

    public long getTotalBytesReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
    }

    public long getTotalBytesSent() {
        return requireLong(VirtualServerProperty.CONNECTION_BYTES_SENT_TOTAL);
    }

    public long getTotalBytesUploaded() {
        return requireLong(VirtualServerProperty.VIRTUALSERVER_TOTAL_BYTES_UPLOADED);
    }

    public int getTotalClientConnections() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_CLIENT_CONNECTIONS);
    }

    public double getTotalControlPacketloss() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_CONTROL);
    }

    public double getTotalKeepAlivePacketloss() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_KEEPALIVE);
    }

    public double getTotalPacketloss() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_TOTAL);
    }

    public long getTotalPacketsReceived() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
    }

    public long getTotalPacketsSent() {
        return requireLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_TOTAL);
    }

    public int getTotalQueryClientConnections() {
        return requireInt(VirtualServerProperty.VIRTUALSERVER_QUERY_CLIENT_CONNECTIONS);
    }

    public double getTotalSpeechPacketloss() {
        return requireDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_SPEECH);
    }

    /** @return the monthly upload quota in MiB, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getUploadQuota() {
        return requireLimit(VirtualServerProperty.VIRTUALSERVER_UPLOAD_QUOTA);
    }

    public String getVersion() {
        return require(VirtualServerProperty.VIRTUALSERVER_VERSION);
    }

    public String getWelcomeMessage() {
        return require(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE);
    }

    public boolean hasPassword() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_FLAG_PASSWORD);
    }

    public boolean isAskingPrivilegeKey() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_ASK_FOR_PRIVILEGEKEY);
    }

    public boolean isLoggingChannel() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_CHANNEL);
    }

    public boolean isLoggingClient() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_CLIENT);
    }

    public boolean isLoggingFileTransfer() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_FILETRANSFER);
    }

    public boolean isLoggingPermissions() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_PERMISSIONS);
    }

    public boolean isLoggingQuery() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_QUERY);
    }

    public boolean isLoggingServer() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_SERVER);
    }

    public boolean isWeblistEnabled() {
        return requireBoolean(VirtualServerProperty.VIRTUALSERVER_WEBLIST_ENABLED);
    }
}
