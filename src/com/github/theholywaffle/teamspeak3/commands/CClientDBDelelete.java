package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientDBDelelete extends Command {

	public CClientDBDelelete(int clientDBId) {
		super("clientdbdelete");
		add(new KeyValueParam("cldbid",clientDBId+""));
	}

}
