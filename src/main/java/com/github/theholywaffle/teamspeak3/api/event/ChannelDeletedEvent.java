package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelDeletedEvent extends Wrapper implements TS3EventEmitter,TS3Event {

	public ChannelDeletedEvent(){
		super(null);
	}
	public ChannelDeletedEvent(HashMap<String, String> map){
		super(map);
	}
	public int getChannelId() {
		return getInt("cid");
	}
	
	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onChannelDeleted(new ChannelDeletedEvent(map));
	}

}
