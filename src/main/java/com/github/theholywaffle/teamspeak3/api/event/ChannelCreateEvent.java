package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelCreateEvent extends Wrapper implements TS3EventEmitter,TS3Event {

	public ChannelCreateEvent(){
		super(null);
	}
	public ChannelCreateEvent(HashMap<String, String> map){
		super(map);
	}
	public int getChannelId() {
		return getInt("cid");
	}
	
	public int getReasonId() {
		return getInt("reasonid");
	}
	
	public int getInvokerId() {
		return getInt("invokerid");
	}
	
	public String getInvokerName(){
		return get("invokername");
	}
	
	public String getInvokerUniqueId(){
		return get("invokeruid");
	}
	@Override
	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onChannelCreate(new ChannelCreateEvent(map));

	}

}
