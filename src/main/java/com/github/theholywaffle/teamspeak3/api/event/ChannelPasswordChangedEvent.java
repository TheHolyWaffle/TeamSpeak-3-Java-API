package com.github.theholywaffle.teamspeak3.api.event;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelPasswordChangedEvent extends Wrapper implements TS3EventEmitter {

	public ChannelPasswordChangedEvent() {
		super(null);
	}
	public ChannelPasswordChangedEvent(HashMap<String, String> map) {
		super(map);
	}
	
	public int getChannelId() {
		return getInt("cid");
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
	public String getInvokerUId(){
		return get("invokeruid");
	}
	@Override
	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onChannelPasswordChanged(new ChannelPasswordChangedEvent(map));

	}

}
