package com.github.theholywaffle.teamspeak3.api.event;

public enum TS3EventType {
	
	SERVER("server"),
	CHANNEL("channel"),
	TEXT_SERVER("textserver"),
	TEXT_CHANNEL("textchannel"),
	TEXT_PRIVATE("textprivate");
	
	private String name;

	TS3EventType(String name){
		this.name=name;
	}
	
	public String toString(){
		return name;
	}

}
