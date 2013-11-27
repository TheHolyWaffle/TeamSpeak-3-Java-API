package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class PermissionInfo extends Wrapper {

	public PermissionInfo(HashMap<String, String> map) {
		super(map);
	}
	
	public int getId(){
		return getInt("permid");
	}
	
	public String getName(){
		return get("permname");
	}

	public String getDescription(){
		return get("permdesc");
	}
	

}
