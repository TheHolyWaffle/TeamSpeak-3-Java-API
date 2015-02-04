package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

import java.util.HashMap;

public abstract class BaseEvent extends Wrapper implements TS3Event {

	public BaseEvent(HashMap<String, String> map) {
		super(map);
	}

	public int getInvokerId() {
		return getInt("invokerid");
	}

	public String getInvokerName() {
		return get("invokername");
	}

	public String getInvokerUniqueId() {
		return get("invokeruid");
	}

	public int getReasonId() {
		return getInt("reasonid");
	}

	public String getReasonMessage() {
		return get("reasonmsg");
	}
}
