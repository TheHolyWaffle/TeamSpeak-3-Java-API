package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CPermFind extends Command{

	public CPermFind(String permName) {
		super("permfind");
		add(new KeyValueParam("permsid",permName));
	}

}
