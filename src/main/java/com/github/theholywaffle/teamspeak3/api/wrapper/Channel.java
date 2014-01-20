/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;

public class Channel extends Wrapper {

	public Channel(HashMap<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt(ChannelProperty.CID);
	}

	public int getParentChannelId() {
		return getInt(ChannelProperty.PID);
	}

	public int getOrder() {
		return getInt(ChannelProperty.CHANNEL_ORDER);
	}

	public String getName() {
		return get(ChannelProperty.CHANNEL_NAME);
	}

	public String getTopic() {
		return get(ChannelProperty.CHANNEL_TOPIC);
	}

	public boolean isDefault() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_DEFAULT);
	}

	public boolean hasPassword() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PASSWORD);
	}

	public boolean isPermanent() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PERMANENT);
	}

	public boolean isSemiPermanent() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT);
	}

	public int getCodec() {
		return getInt(ChannelProperty.CHANNEL_CODEC);
	}

	public int getCodecQuality() {
		return getInt(ChannelProperty.CHANNEL_CODEC_QUALITY);
	}

	public int getNeededTalkPower() {
		return getInt(ChannelProperty.CHANNEL_NEEDED_TALK_POWER);
	}

	public long getIconId() {
		return getLong(ChannelProperty.CHANNEL_ICON_ID);
	}

	public int getTotalClientsFamily() {
		return getInt("total_clients_family");
	}

	public int getMaxClients() {
		return getInt(ChannelProperty.CHANNEL_MAXCLIENTS);
	}

	public int getMaxFamilyClients() {
		return getInt(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS);
	}

	public int getTotalClients() {
		return getInt("total_clients");
	}

	public int getNeededSubscribePower() {
		return getInt(ChannelProperty.CHANNEL_NEEDED_SUBSCRIBE_POWER);

	}

}
