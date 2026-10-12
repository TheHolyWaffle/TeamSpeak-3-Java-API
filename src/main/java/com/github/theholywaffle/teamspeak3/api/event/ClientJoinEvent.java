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

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

import java.util.List;
import java.util.Optional;

public class ClientJoinEvent extends BaseEvent {

	public ClientJoinEvent(Wrapper wrapper) {
		super(wrapper);
	}

	public int getClientFromId() {
		return requireInt("cfid");
	}

	public int getClientTargetId() {
		return requireInt("ctid");
	}

	public ClientId getClientId() {
		return requireClientId("clid");
	}

	public String getUniqueClientIdentifier() {
		return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}

	public String getClientNickname() {
		return require(ClientProperty.CLIENT_NICKNAME);
	}

	public boolean isClientInputMuted() {
		return requireBoolean(ClientProperty.CLIENT_INPUT_MUTED);
	}

	public boolean isClientOutputMuted() {
		return requireBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
	}

	public boolean isClientOutputOnlyMuted() {
		return requireBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
	}

	public boolean isClientUsingHardwareInput() {
		return requireBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
	}

	public boolean isClientUsingHardwareOutput() {
		return requireBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
	}

	public String getClientMetadata() {
		return require(ClientProperty.CLIENT_META_DATA);
	}

	public boolean isClientRecording() {
		return requireBoolean(ClientProperty.CLIENT_IS_RECORDING);
	}

	public ClientDatabaseId getClientDatabaseId() {
		return requireDatabaseId(ClientProperty.CLIENT_DATABASE_ID);
	}

	public int getClientChannelGroupId() {
		return requireInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
	}

	public int getAmountOfServerGroups() {
		return getClientServerGroups().size();
	}

	/** @return the IDs of the client's server groups, in received order */
	public List<Integer> getClientServerGroups() {
		return requireIntList(ClientProperty.CLIENT_SERVERGROUPS);
	}

	public boolean isClientAway() {
		return requireBoolean(ClientProperty.CLIENT_AWAY);
	}

	public String getClientAwayMessage() {
		return require(ClientProperty.CLIENT_AWAY_MESSAGE);
	}

	public int getClientType() {
		return requireInt(ClientProperty.CLIENT_TYPE);
	}

	/** @return the avatar hash, or empty if the client has no avatar */
	public Optional<String> getClientFlagAvatarId() {
		return findText(ClientProperty.CLIENT_FLAG_AVATAR);
	}

	public int getClientTalkPower() {
		return requireInt(ClientProperty.CLIENT_TALK_POWER);
	}

	/** {@code client_talk_request} holds the request time in epoch seconds, or {@code 0} without a request. */
	public boolean isClientRequestingToTalk() {
		return requireTimestamp(ClientProperty.CLIENT_TALK_REQUEST).isPresent();
	}

	public String getClientTalkRequestMessage() {
		return require(ClientProperty.CLIENT_TALK_REQUEST_MSG);
	}

	public String getClientDescription() {
		return require(ClientProperty.CLIENT_DESCRIPTION);
	}

	public boolean isClientTalking() {
		return requireBoolean(ClientProperty.CLIENT_IS_TALKER);
	}

	public boolean isClientPrioritySpeaker() {
		return requireBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
	}

	public int getClientUnreadMessages() {
		return requireInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
	}

	public String getClientPhoneticNickname() {
		return require(ClientProperty.CLIENT_NICKNAME_PHONETIC);
	}

	public int getClientNeededServerQueryViewPower() {
		return requireInt(ClientProperty.CLIENT_NEEDED_SERVERQUERY_VIEW_POWER);
	}

	public long getClientIconId() {
		return requireLong(ClientProperty.CLIENT_ICON_ID);
	}

	public boolean isClientChannelCommander() {
		return requireBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
	}

	/** @return the two-letter country code, or empty if the server could not determine it */
	public Optional<String> getClientCountry() {
		return findText(ClientProperty.CLIENT_COUNTRY);
	}

	public int getClientInheritedChannelGroupId() {
		return requireInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onClientJoin(this);
	}
}
