package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class AdvancedPermission extends Wrapper{

	public AdvancedPermission(HashMap<String, String> map) {
		super(map);
	}
	
	public int getType(){
		return getInt("t");
	}
	
	public int getId1(){
		return getInt("id1");
	}
	
	public int getId2(){
		return getInt("id2");
	}
	
	public int getPermissionId(){
		return getInt("p");
	}
	
	public int getPermissionValue(){
		return getInt("v");
	}
	
	public boolean isNegated(){
		return getBoolean("n");
	}
	
	public boolean isSkipped(){
		return getBoolean("s");
	}



}
