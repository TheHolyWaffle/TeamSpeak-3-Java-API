package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class ServerGroupClient extends Wrapper{

	public ServerGroupClient(HashMap<String, String> map) {
		super(map);
	}
	
	public int getClientDatabaseId(){
		return getInt("cldbid");
	}
	
	public String getNickname(){
		return get("client_nickname");
	}
	
	public String getUniqueIdentifier(){
		return get("client_unique_identifier");
	}

}
