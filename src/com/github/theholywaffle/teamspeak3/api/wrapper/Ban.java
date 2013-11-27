package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

public class Ban extends Wrapper{

	public Ban(HashMap<String, String> map) {
		super(map);
	}
	
	public int getId(){
		return getInt("banid");
	}
	
	public String getBannedIp(){
		return get("ip");
	}
	
	public String getBannedName(){
		return get("name");
	}
	
	public String getBannedUId(){
		return get("uid");
	}
	
	public String getLastNickname(){
		return get("lastnickname");
	}
	
	public Date getCreatedDate(){
		return new Date(getLong("created")*1000);
	}
	
	public long getDuration(){
		return getLong("duration");
	}
	
	public String getInvokerName(){
		return get("invokername");
	}
	
	public int getInvokerClientDBId(){
		return getInt("invokercldbid");
	}
	
	public String getInvokerUId(){
		return get("invokeruid");
	}
	
	public String getReason(){
		return get("reason");
	}
	
	public int getEnforcements(){
		return getInt("enforcements");
	}
}
