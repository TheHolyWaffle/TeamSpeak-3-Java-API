package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.StringUtil;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;

public class ClientInfo extends Wrapper {

	public ClientInfo(HashMap<String, String> map) {
		super(map);
	}

	public boolean canTalk() {
		return getBoolean(ClientProperty.CLIENT_IS_TALKER);
	}

	public String getAvatar() {
		return get(ClientProperty.CLIENT_FLAG_AVATAR);
	}

	public String getAwayMessage() {
		return get(ClientProperty.CLIENT_AWAY_MESSAGE);
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

	public int getChannelGroupId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
	}

	public int getChannelGroupInheritedChannelId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
	}

	public int getChannelId() {
		return getInt(ClientProperty.CID);
	}

	public String getCountry() {
		return get(ClientProperty.CLIENT_COUNTRY);
	}

	public Date getCreatedDate() {
		return new Date(getLong(ClientProperty.CLIENT_CREATED) * 1000);
	}

	public int getDatabaseId() {
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}

	public int getDefaultChannel() {
		return StringUtil.getInt(get(ClientProperty.CLIENT_DEFAULT_CHANNEL)
				.substring(1));
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

	public long getIconId() {
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}

	public long getIdleTime() {
		return getLong(ClientProperty.CLIENT_IDLE_TIME);
	}

	public String getIp() {
		return get(ClientProperty.CONNECTION_CLIENT_IP);
	}

	public Date getLastConnectedDate() {
		return new Date(getLong(ClientProperty.CLIENT_LASTCONNECTED) * 1000);
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

	public String getNickname() {
		return get(ClientProperty.CLIENT_NICKNAME);
	}

	public String getPhoneticNickname() {
		return get(ClientProperty.CLIENT_NICKNAME_PHONETIC);
	}

	public String getPlatform() {
		return get(ClientProperty.CLIENT_PLATFORM);
	}

	public int[] getServerGroups() {
		String str = get(ClientProperty.CLIENT_SERVERGROUPS);
		String[] arr = str.split(",");
		int[] groups = new int[arr.length];
		for (int i = 0; i < groups.length; i++) {
			groups[i] = StringUtil.getInt(arr[i]);
		}
		return groups;
	}

	public int getTalkPower() {
		return getInt(ClientProperty.CLIENT_TALK_POWER);
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

	public int getType() {
		return getInt(ClientProperty.CLIENT_TYPE);
	}

	public String getUniqueIdentifier() {
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}

	public int getUnreadMessages() {
		return getInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
	}

	public String getVersion() {
		return get(ClientProperty.CLIENT_VERSION);
	}

	public boolean isAway() {
		return getBoolean(ClientProperty.CLIENT_AWAY);
	}

	public boolean isChannelCommander() {
		return getBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
	}

	public boolean isInputHardware() {
		return getBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
	}

	public boolean isInputMuted() {
		return getBoolean(ClientProperty.CLIENT_INPUT_MUTED);
	}

	public boolean isOutputHardware() {
		return getBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
	}

	public boolean isOutputMuted() {
		return getBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
	}

	public boolean isOutputOnlyMuted() {
		return getBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
	}

	public boolean isPrioritySpeaker() {
		return getBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
	}

	public boolean isRecording() {
		return getBoolean(ClientProperty.CLIENT_IS_RECORDING);
	}

	public boolean isRequestingToTalk() {
		return getBoolean(ClientProperty.CLIENT_TALK_REQUEST);
	}
}
