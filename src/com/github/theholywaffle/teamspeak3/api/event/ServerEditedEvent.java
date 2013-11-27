package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ServerEditedEvent extends Wrapper implements TS3Event,
		TS3EventEmitter {

	public ServerEditedEvent(HashMap<String, String> map) {
		super(map);
	}

	public ServerEditedEvent() {
		super(null);
	}
	
	public int getReasonId(){
		return getInt("reasonid");
	}
	
	public int getInvokerId(){
		return getInt("invokerid");
	}
	
	public String getInvokerName(){
		return get("invokername");
	}
	
	public String getInvokerUniqueId(){
		return get("invokeruid");
	}

	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onServerEdit(new ServerEditedEvent(map));
	}

}
