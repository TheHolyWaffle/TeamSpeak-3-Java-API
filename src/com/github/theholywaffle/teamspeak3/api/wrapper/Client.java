package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.StringUtil;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;

public class Client  extends Wrapper{

	public Client(HashMap<String, String> map) {
		super(map);
	}
	
	public boolean canTalk(){
		return getBoolean(ClientProperty.CLIENT_IS_TALKER);
	}
	
	public String getAwayMessage(){
		return get(ClientProperty.CLIENT_AWAY_MESSAGE);
	}
	
	public int getChannelGroupId(){
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
	}
	
	public int getChannelId(){
		return getInt(ClientProperty.CID);
	}

	public String getCountry(){
		return get(ClientProperty.CLIENT_COUNTRY);
	}
	
	public Date getCreatedDate(){
		return new Date(getLong(ClientProperty.CLIENT_CREATED)*1000);
	}
	
	public int getDatabaseId(){
		return getInt(ClientProperty.CLIENT_DATABASE_ID);
	}
	
	public long getIconId(){
		return getLong(ClientProperty.CLIENT_ICON_ID);
	}
	
	public int getId(){
		return getInt("clid");
	}
	
	public long getIdleTime(){
		return getLong(ClientProperty.CLIENT_IDLE_TIME);
	}
	
	public int getInheritedChannelGroupId(){
		return getInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
	}
	
	public Date getLastConnectedDate(){
		return new Date(getLong(ClientProperty.CLIENT_LASTCONNECTED)*1000);
	}
	
	public String getNickname(){
		return get(ClientProperty.CLIENT_NICKNAME);
	}
	
	public String getPlatform(){
		return get(ClientProperty.CLIENT_PLATFORM);
	}
	
	public int[] getServerGroups(){
		String str = get(ClientProperty.CLIENT_SERVERGROUPS);
		String[] arr = str.split(",");
		int[] groups = new int[arr.length];
		for(int i=0;i<groups.length;i++){
			groups[i] = StringUtil.getInt(arr[i]);
		}
		return groups;
	}
	
	public int getTalkPower(){
		return getInt(ClientProperty.CLIENT_TALK_POWER);
	}
	
	public int getType(){
		return getInt(ClientProperty.CLIENT_TYPE);
	}
	
	public String getUniqueIdentifier(){
		return get(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
	}
	
	public String getVersion(){
		return get(ClientProperty.CLIENT_VERSION);
	}
	
	public boolean isAway(){
		return getBoolean(ClientProperty.CLIENT_AWAY);
	}
	
	public boolean isChannelCommander(){
		return getBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
	}
	
	public boolean isInputHardware(){
		return getBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
	}
	
	public boolean isInputMuted(){
		return getBoolean(ClientProperty.CLIENT_INPUT_MUTED);
	}
	
	public boolean isOutputHardware(){
		return getBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
	}
	
	public boolean isOutputMuted(){
		return getBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
	}
	
	public boolean isPrioritySpeaker(){
		return getBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
	}
	
	public boolean isRecording(){
		return getBoolean(ClientProperty.CLIENT_IS_RECORDING);
	}
	
	public boolean isTalking(){
		return getBoolean("client_flag_talking");
	}
}
