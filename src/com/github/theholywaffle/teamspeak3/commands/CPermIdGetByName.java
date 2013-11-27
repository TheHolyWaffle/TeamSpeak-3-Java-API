package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CPermIdGetByName extends Command{

	public CPermIdGetByName(String permName) {
		super("permidgetbyname");
		add(new KeyValueParam("permsid",permName));
	}

}
