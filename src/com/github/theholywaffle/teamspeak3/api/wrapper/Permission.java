package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class Permission extends Wrapper {

	public Permission(HashMap<String, String> map) {
		super(map);
	}

	public String getName(){
		return get("permsid");
	}
	
	public int getValue(){
		return getInt("permvalue");
	}
	
	public boolean isNegated(){
		return getInt("permnegated") == 1;
	}
	
	public boolean isSkipped(){
		return getInt("permskip") == 1;
	}

}
