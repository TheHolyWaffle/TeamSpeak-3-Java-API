package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientPoke extends Command {

	public CClientPoke(int clientId, String message) {
		super("clientpoke");
		add(new KeyValueParam("clid", clientId));
		add(new KeyValueParam("msg", message));
	}

}
