package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CServerDelete extends Command {

	public CServerDelete(int id) {
		super("serverdelete");
		add(new KeyValueParam("sid", id));
	}

}
