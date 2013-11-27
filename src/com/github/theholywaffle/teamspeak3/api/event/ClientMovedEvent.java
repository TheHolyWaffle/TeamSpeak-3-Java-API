package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ClientMovedEvent extends Wrapper implements TS3Event,
		TS3EventEmitter {

	public ClientMovedEvent() {
		super(null);
	}

	public ClientMovedEvent(HashMap<String, String> map) {
		super(map);
	}
	
	public int getClientTargetId(){
		return getInt("ctid");
	}
	
	public int getReasonId(){
		return getInt("reasonid"); 
	}
	
	public int getClientId(){
		return getInt("clid"); 
	}

	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onClientMoved(new ClientMovedEvent(map));		
	}

}
