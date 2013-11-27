package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Date;
import java.util.HashMap;

public class Message extends Wrapper{


	public Message(HashMap<String, String> map) {
		super(map);
	}

	public int getId(){
		return getInt("msgid");
	}
	
	public String getClientUniqueIdentifier(){
		return get("cluid");
	}
	
	public String getSubject(){
		return get("subject");
	}
	
	public Date getReceivedDate(){
		return new Date(getLong("timestamp")*1000);
	}
	
	public boolean hasRead(){
		return getBoolean("flag_read");
	}

}
