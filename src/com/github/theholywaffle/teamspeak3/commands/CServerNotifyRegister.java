package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerNotifyRegister extends Command {

	public CServerNotifyRegister(TS3EventType t) {
		this(t, -1);
	}

	public CServerNotifyRegister(TS3EventType t, int channelId) {
		super("servernotifyregister");
		add(new KeyValueParam("event", t.toString()));
		if (channelId >= 0) {
			add(new KeyValueParam("id", channelId));
		}

	}

}
