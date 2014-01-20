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

public class ChannelInfo  extends Wrapper{

	public ChannelInfo(HashMap<String, String> map) {
		super(map);
	}
	
	public int getParentChannelId(){
		return getInt(ChannelProperty.PID);
	}
	
	public String getName(){
		return get(ChannelProperty.CHANNEL_NAME);
	}
	
	public String getTopic(){
		return get(ChannelProperty.CHANNEL_TOPIC);
	}
	
	public String getDescription(){
		return get(ChannelProperty.CHANNEL_DESCRIPTION);
	}
	
	public String getPassword(){
		return get(ChannelProperty.CHANNEL_PASSWORD);
	}
	
	public int getCodec(){
		return getInt(ChannelProperty.CHANNEL_CODEC);
	}
	
	public int getCodecQuality(){
		return getInt(ChannelProperty.CHANNEL_CODEC_QUALITY);
	}
	
	public int getMaxClients(){
		return getInt(ChannelProperty.CHANNEL_MAXCLIENTS);
	}
	
	public int getMaxFamilyClients(){
		return getInt(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS);
	}
	
	public int getOrder(){
		return getInt(ChannelProperty.CHANNEL_ORDER);
	}
	
	public boolean isPermanent(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PERMANENT);
	}
	
	public boolean isSemiPermanent(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT);
	}
	
	public boolean isDefault(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_DEFAULT);
	}
	
	public boolean hasPassword(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PASSWORD);
	}
	
	public int getCodecLatencyFactor(){
		return getInt(ChannelProperty.CHANNEL_CODEC_LATENCY_FACTOR);
	}
	
	public boolean isEncrypted(){
		return !getBoolean(ChannelProperty.CHANNEL_CODEC_IS_UNENCRYPTED);
	}
	
	public boolean hasUnlimitedClients(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED);
	}
	
	public boolean hasUnlimitedFamilyClients(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_UNLIMITED);
	}
	
	public boolean hasInheritedMaxFamilyClients(){
		return getBoolean(ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED);
	}
	
	public String getFilePath(){
		return get(ChannelProperty.CHANNEL_FILEPATH);
	}
	
	public int getNeededTalkPower(){
		return getInt(ChannelProperty.CHANNEL_NEEDED_TALK_POWER);
	}
	
	public boolean isForcedSilence(){
		return getBoolean(ChannelProperty.CHANNEL_FORCED_SILENCE);
	}
	
	public String getPhoneticName(){
		return get(ChannelProperty.CHANNEL_NAME_PHONETIC);
	}

	public long getIconId(){
		return getLong(ChannelProperty.CHANNEL_ICON_ID);
	}

}
