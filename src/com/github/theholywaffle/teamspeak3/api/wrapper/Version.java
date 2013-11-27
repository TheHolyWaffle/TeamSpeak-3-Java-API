package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class Version extends Wrapper{

	public Version(HashMap<String, String> map) {
		super(map);
	}
	
	public String getVersion(){
		return get("version");
	}
	
	public String getBuild(){
		return get("build");
	}
	
	public String getPlatform(){
		return get("platform");
	}

}
