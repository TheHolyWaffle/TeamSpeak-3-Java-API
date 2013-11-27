package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CClientFind extends Command{

	public CClientFind(String pattern) {
		super("clientfind");
		add(new KeyValueParam("pattern",pattern));
	}

}
