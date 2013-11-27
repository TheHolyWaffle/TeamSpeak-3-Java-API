package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class ChannelGroupClient extends Wrapper{

	public ChannelGroupClient(HashMap<String, String> map) {
		super(map);
	}
	
	public int getChannelId(){
		return getInt("cid");
	}
	
	public int getClientDatabaseId(){
		return getInt("cldbid");
	}
	
	public int getChannelGroupId(){
		return getInt("cgid");
	}


}
