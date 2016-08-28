package com.github.theholywaffle.teamspeak3.api.event;

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

import java.util.Map;

public class ClientJoinEvent extends BaseEvent {

	public ClientJoinEvent(Map<String, String> map) {
		super(map);
	}

	public int getClientFromId() {
		return getInt("cfid");
	}

	public int getClientTargetId() {
		return getInt("ctid");
	}

	public int getClientId() {
		return getInt("clid");
	}

	public String getUniqueClientIdentifier() {
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}

	public String getClientNickname() {
		return get(ClientProperty.CLIENT_NICKNAME);
	}

	public boolean isClientInputMuted() {
		return getBoolean(ClientProperty.CLIENT_INPUT_MUTED);
	}

	public boolean isClientOutputMuted() {
		return getBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
	}

	public boolean isClientOutputOnlyMuted() {
		return getBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
	}

	public boolean isClientUsingHardwareInput() {
		return getBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
	}

	public boolean isClientUsingHardwareOutput() {
		return getBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
	}

	public String getClientMetadata() {
		return get(ClientProperty.CLIENT_META_DATA);
	}

	public boolean isClientRecording() {
		return getBoolean(ClientProperty.CLIENT_IS_RECORDING);
	}

	public int getClientDatabaseId() {
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}

	public int getClientChannelGroupId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
	}

	public int getAmountOfServerGroups() {
		//getInt was turning the String 1,2,3,.. to a int which wasn't right.
		//Now it gets the Amount even if the ID is <=10
		String[] split = get(ClientProperty.CLIENT_SERVERGROUPS).split(",");
		return split.length;
	}

	public String getClientServerGroups() {
		//getClientServerGroups returns a string containing the server group ID for example 1,2,3,...
		return get(ClientProperty.CLIENT_SERVERGROUPS);
	}

	public boolean isClientAway() {
		return getBoolean(ClientProperty.CLIENT_AWAY);
	}

	public String getClientAwayMessage() {
		return get(ClientProperty.CLIENT_AWAY_MESSAGE);
	}

	public int getClientType() {
		return getInt(ClientProperty.CLIENT_TYPE);
	}

	public String getClientFlagAvatarId() {
		return get(ClientProperty.CLIENT_FLAG_AVATAR);
	}

	public int getClientTalkPower() {
		return getInt(ClientProperty.CLIENT_TALK_POWER);
	}

	public boolean isClientRequestingToTalk() {
		return getBoolean(ClientProperty.CLIENT_TALK_REQUEST);
	}

	public String getClientTalkRequestMessage() {
		return get(ClientProperty.CLIENT_TALK_REQUEST_MSG);
	}

	public String getClientDescription() {
		return get(ClientProperty.CLIENT_DESCRIPTION);
	}

	public boolean isClientTalking() {
		return getBoolean(ClientProperty.CLIENT_IS_TALKER);
	}

	public boolean isClientPrioritySpeaker() {
		return getBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
	}

	public int getClientUnreadMessages() {
		return getInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
	}

	public String getClientPhoneticNickname() {
		return get(ClientProperty.CLIENT_NICKNAME_PHONETIC);
	}

	public int getClientNeededServerQueryViewPower() {
		return getInt(ClientProperty.CLIENT_NEEDED_SERVERQUERY_VIEW_POWER);
	}

	public long getClientIconId() {
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}

	public boolean isClientChannelCommander() {
		return getBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
	}

	public String getClientCountry() {
		return get(ClientProperty.CLIENT_COUNTRY);
	}

	public int getClientInheritedChannelGroupId() {
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onClientJoin(this);
	}
}
