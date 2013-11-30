package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerIdGetByPort extends Command {

	public CServerIdGetByPort(int port) {
		super("serveridgetbyport");
		add(new KeyValueParam("virtualserver_port", port));
	}

}
