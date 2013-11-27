package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

public class PrivilegeKey extends Wrapper {

	public PrivilegeKey(HashMap<String, String> map) {
		super(map);
	}
	
	public String getToken(){
		return get("token");
	}
	
	public int getType(){
		return getInt("token_type");
	}
	
	public boolean isServerGroupToken(){
		return getType()==0;
	}
	
	public boolean isChannelGroupToken(){
		return !isServerGroupToken();
	}
	
	public int getGroupId(){
		return getInt("token_id1");
	}
	
	public int getChannelId(){
		return getInt("token_id2");
	}
	
	public Date getCreated(){
		return new Date(getLong("token_created")*1000);
	}

	public String getDescription(){
		return get("token_description");
	}
}
