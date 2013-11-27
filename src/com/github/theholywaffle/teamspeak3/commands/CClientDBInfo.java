package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientDBInfo extends Command {

	public CClientDBInfo(int clientDBId) {
		super("clientdbinfo");
		add(new KeyValueParam("cldbid",clientDBId+""));
	}

}
