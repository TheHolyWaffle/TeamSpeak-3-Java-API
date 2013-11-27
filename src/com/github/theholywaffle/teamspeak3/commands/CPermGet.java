package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CPermGet extends Command{

	public CPermGet(String permName) {
		super("permget");
		add(new KeyValueParam("permsid",permName));
	}

}
