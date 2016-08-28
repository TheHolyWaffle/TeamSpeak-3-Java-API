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

import java.util.Date;
import java.util.Map;

public class VirtualServerInfo extends VirtualServer {

	public VirtualServerInfo(Map<String, String> map) {
		super(map);
	}

	public int getAntifloodPointsNeededCommandBlock() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_NEEDED_COMMAND_BLOCK);
	}

	public int getAntifloodPointsNeedIpBlock() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_NEEDED_IP_BLOCK);
	}

	public int getAntifloodPointsTickReduce() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_ANTIFLOOD_POINTS_TICK_REDUCE);
	}

	public long getBandwidthReceivedLastMinute() {
		return getInt(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthReceivedLastSecond() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
	}

	public long getBandwidthSentLastMinute() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
	}

	public long getBandwidthSentLastSecond() {
		return getLong(VirtualServerProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
	}

	public int getChannelsOnline() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_CHANNELSONLINE);
	}

	public CodecEncryptionMode getCodecEncryptionMode() {
		final int encryptionMode = getInt(VirtualServerProperty.VIRTUALSERVER_CODEC_ENCRYPTION_MODE);
		for (final CodecEncryptionMode m : CodecEncryptionMode.values()) {
			if (m.getIndex() == encryptionMode) {
				return m;
			}
		}
		return CodecEncryptionMode.UNKNOWN;
	}

	public int getComplaintAutobanCount() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_AUTOBAN_COUNT);
	}

	public long getComplaintAutobanTime() {// SEC
		return getLong(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_AUTOBAN_TIME);
	}

	public long getComplaintRemoveTime() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_COMPLAIN_REMOVE_TIME);
	}

	public long getControlBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_CONTROL);
	}

	public long getControlBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_SENT_CONTROL);
	}

	public long getControlPacketsReceived() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_CONTROL);
	}

	public long getControlPacketsSent() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_CONTROL);
	}

	public Date getCreatedDate() {
		return new Date(getLong(VirtualServerProperty.VIRTUALSERVER_CREATED) * 1000);
	}

	public int getDefaultChannelAdminGroup() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_CHANNEL_ADMIN_GROUP);
	}

	public int getDefaultChannelGroup() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_CHANNEL_GROUP);
	}

	public int getDefaultServerGroup() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_DEFAULT_SERVER_GROUP);
	}

	public long getDownloadQuota() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_DOWNLOAD_QUOTA);
	}

	public String getFileBase() {
		return get(VirtualServerProperty.VIRTUALSERVER_FILEBASE);
	}

	public long getFiletransferBandwidthReceived() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
	}

	public long getFiletransferBandwidthSent() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
	}

	public long getFiletransferBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL);
	}

	public long getFiletransferBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL);
	}

	public int getHostbannerGfxInterval() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_GFX_INTERVAL);
	}

	public String getHostbannerGfxUrl() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_GFX_URL);
	}

	public HostBannerMode getHostbannerMode() {
		final int hostbannerMode = getInt(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_MODE);
		for (final HostBannerMode m : HostBannerMode.values()) {
			if (m.getIndex() == hostbannerMode) {
				return m;
			}
		}
		return HostBannerMode.UNKNOWN;
	}

	public String getHostbannerUrl() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTBANNER_URL);
	}

	public String getHostbuttonGfxUrl() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_GFX_URL);
	}

	public String getHostbuttonTooltip() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_TOOLTIP);
	}

	public String getHostbuttonUrl() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTBUTTON_URL);
	}

	public String getHostMessage() {
		return get(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE);
	}

	public HostMessageMode getHostMessageMode() {
		final int hostmessageMode = getInt(VirtualServerProperty.VIRTUALSERVER_HOSTMESSAGE_MODE);
		for (final HostMessageMode m : HostMessageMode.values()) {
			if (m.getIndex() == hostmessageMode) {
				return m;
			}
		}
		return HostMessageMode.UNKNOWN;
	}

	public long getIconId() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_ICON_ID);
	}

	public String getIp() {
		return get(VirtualServerProperty.VIRTUALSERVER_IP);
	}

	public long getKeepAliveBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_KEEPALIVE);
	}

	public long getKeepAliveBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_SENT_KEEPALIVE);
	}

	public long getKeepAlivePacketsReceived() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_KEEPALIVE);
	}

	public long getKeepAlivePacketsSent() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_KEEPALIVE);
	}

	public String getMachineId() {
		return get(VirtualServerProperty.VIRTUALSERVER_MACHINE_ID);
	}

	public long getMaxDownloadBandwidth() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_MAX_DOWNLOAD_TOTAL_BANDWIDTH);
	}

	public long getMaxUploadBandwidth() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_MAX_UPLOAD_TOTAL_BANDWIDTH);
	}

	public int getMinClientsInChannelBeforeForcedSilence() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_MIN_CLIENTS_IN_CHANNEL_BEFORE_FORCED_SILENCE);
	}

	public int getMinimumClientVersion() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_MIN_CLIENT_VERSION);
	}

	public long getMonthlyBytesDownloaded() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_MONTH_BYTES_DOWNLOADED);
	}

	public long getMonthlyBytesUploaded() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_MONTH_BYTES_UPLOADED);
	}

	public int getNeededIdentitySecurityLevel() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_NEEDED_IDENTITY_SECURITY_LEVEL);
	}

	public String getPassword() {
		return get(VirtualServerProperty.VIRTUALSERVER_PASSWORD);
	}

	public String getPhoneticName() {
		return get(VirtualServerProperty.VIRTUALSERVER_NAME_PHONETIC);
	}

	public double getPing() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PING);
	}

	public String getPlatform() {
		return get(VirtualServerProperty.VIRTUALSERVER_PLATFORM);
	}

	public double getPrioritySpeakerDimmModificator() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_PRIORITY_SPEAKER_DIMM_MODIFICATOR);
	}

	public int getReservedSlots() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_RESERVED_SLOTS);
	}

	public long getSpeechBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_SPEECH);
	}

	public long getSpeechBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_SENT_SPEECH);
	}

	public long getSpeechPacketsReceived() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_SPEECH);
	}

	public long getSpeechPacketsSent() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_SPEECH);
	}

	public long getTotalBytesDownloaded() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_TOTAL_BYTES_DOWNLOADED);
	}

	public long getTotalBytesReceived() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
	}

	public long getTotalBytesSent() {
		return getLong(VirtualServerProperty.CONNECTION_BYTES_SENT_TOTAL);
	}

	public long getTotalBytesUploaded() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_TOTAL_BYTES_UPLOADED);
	}

	public int getTotalClientConnections() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_CLIENT_CONNECTIONS);
	}

	public double getTotalControlPacketloss() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_CONTROL);
	}

	public double getTotalKeepAlivePacketloss() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_KEEPALIVE);
	}

	public double getTotalPacketloss() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_TOTAL);
	}

	public long getTotalPacketsReceived() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
	}

	public long getTotalPacketsSent() {
		return getLong(VirtualServerProperty.CONNECTION_PACKETS_SENT_TOTAL);
	}

	public int getTotalQueryClientConnections() {
		return getInt(VirtualServerProperty.VIRTUALSERVER_QUERY_CLIENT_CONNECTIONS);
	}

	public double getTotalSpeechPacketloss() {
		return getDouble(VirtualServerProperty.VIRTUALSERVER_TOTAL_PACKETLOSS_SPEECH);
	}

	public long getUploadQuota() {
		return getLong(VirtualServerProperty.VIRTUALSERVER_UPLOAD_QUOTA);
	}

	public String getVersion() {
		return get(VirtualServerProperty.VIRTUALSERVER_VERSION);
	}

	public String getWelcomeMessage() {
		return get(VirtualServerProperty.VIRTUALSERVER_WELCOMEMESSAGE);
	}

	public boolean hasPassword() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_FLAG_PASSWORD);
	}

	public boolean isAskingPrivilegeKey() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_ASK_FOR_PRIVILEGEKEY);
	}

	public boolean isLoggingChannel() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_CHANNEL);
	}

	public boolean isLoggingClient() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_CLIENT);
	}

	public boolean isLoggingFileTransfer() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_FILETRANSFER);
	}

	public boolean isLoggingPermissions() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_PERMISSIONS);
	}

	public boolean isLoggingQuery() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_QUERY);
	}

	public boolean isLoggingServer() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_LOG_SERVER);
	}

	public boolean isWeblistEnabled() {
		return getBoolean(VirtualServerProperty.VIRTUALSERVER_WEBLIST_ENABLED);
	}
}
