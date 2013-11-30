package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientInfo extends Command {

	public CClientInfo(int clientId) {
		super("clientinfo");
		add(new KeyValueParam("clid", clientId));
	}

}
