package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerStop extends Command {

	public CServerStop(int id) {
		super("serverstop");
		add(new KeyValueParam("sid", id));
	}
}
