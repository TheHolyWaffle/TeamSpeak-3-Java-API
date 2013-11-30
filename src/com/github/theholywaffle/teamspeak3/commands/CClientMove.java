package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientMove extends Command {

	public CClientMove(int clientId, int channelid, String channelPassword) {
		super("clientmove");
		add(new KeyValueParam("clid", clientId));
		add(new KeyValueParam("cid", channelid));
		if (channelPassword != null) {
			add(new KeyValueParam("cpw", channelPassword));
		}
	}

}
