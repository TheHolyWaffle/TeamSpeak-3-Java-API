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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

public class Client extends Wrapper {

	public Client(Map<String, String> map) {
		super(map);
	}

	public boolean canTalk() {
		return getBoolean(ClientProperty.CLIENT_IS_TALKER);
	}

	public String getAwayMessage() {
		return get(ClientProperty.CLIENT_AWAY_MESSAGE);
	}

	public int getChannelGroupId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
	}

	public int getChannelId() {
		return getInt(ClientProperty.CID);
	}

	/**
	 * Utility method. This method will return a client URI.
	 * A client URI can be used to reference a client in descriptions or just via chat.
	 * Example: {@code client://<clientId>/<clientUId>~<clientNickname>}
	 *
	 * @return Client's URI
	 */
	public String getClientURI() {
		StringBuilder sb = new StringBuilder();
		sb.append("client://").append(getId()).append('/');
		sb.append(getUniqueIdentifier()).append('~');
		try {
			// We will encode the nickname, so characters like spaces work with this.
			sb.append(URLEncoder.encode(getNickname(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM doesn't support UTF-8", e);
		}
		return sb.toString();
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

	public long getIconId() {
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}

	public int getId() {
		return getInt("clid");
	}

	public long getIdleTime() {
		return getLong(ClientProperty.CLIENT_IDLE_TIME);
	}

	public int getInheritedChannelGroupId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
	}

	public String getIp() {
		return get(ClientProperty.CONNECTION_CLIENT_IP);
	}

	public Date getLastConnectedDate() {
		return new Date(getLong(ClientProperty.CLIENT_LASTCONNECTED) * 1000);
	}

	public String getNickname() {
		return get(ClientProperty.CLIENT_NICKNAME);
	}

	public String getPlatform() {
		return get(ClientProperty.CLIENT_PLATFORM);
	}

	public int[] getServerGroups() {
		final String str = get(ClientProperty.CLIENT_SERVERGROUPS);
		final String[] arr = str.split(",");
		final int[] groups = new int[arr.length];
		for (int i = 0; i < groups.length; i++) {
			groups[i] = Integer.parseInt(arr[i]);
		}
		return groups;
	}

	public int getTalkPower() {
		return getInt(ClientProperty.CLIENT_TALK_POWER);
	}

	public int getType() {
		return getInt(ClientProperty.CLIENT_TYPE);
	}

	public String getUniqueIdentifier() {
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
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

	/**
	 * Utility method that does a linear search on the array of server group IDs returned
	 * by {@link #getServerGroups()} and returns {@code true} if that array contains
	 * the given server group ID.
	 *
	 * @param serverGroupId
	 * 		the ID of the server group to search for
	 *
	 * @return whether this client is a member of the given server group
	 */
	public boolean isInServerGroup(int serverGroupId) {
		int[] serverGroupIds = getServerGroups();
		for (int s : serverGroupIds) {
			if (s == serverGroupId) return true;
		}
		return false;
	}

	/**
	 * Utility method that does a linear search on the array of server group IDs returned
	 * by {@link #getServerGroups()} and returns {@code true} if that array contains
	 * the ID of the given server group.
	 *
	 * @param serverGroup
	 * 		the server group to search for
	 *
	 * @return whether this client is a member of the given server group
	 */
	public boolean isInServerGroup(ServerGroup serverGroup) {
		return isInServerGroup(serverGroup.getId());
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

	public boolean isPrioritySpeaker() {
		return getBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
	}

	public boolean isRecording() {
		return getBoolean(ClientProperty.CLIENT_IS_RECORDING);
	}

	public boolean isRegularClient() {
		return getType() == 0;
	}

	public boolean isServerQueryClient() {
		return getType() == 1;
	}

	public boolean isTalking() {
		return getBoolean("client_flag_talking");
	}
}
