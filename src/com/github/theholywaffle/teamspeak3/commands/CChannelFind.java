package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelFind extends Command {

	public CChannelFind(String pattern) {
		super("channelfind");
		add(new KeyValueParam("pattern",pattern));
	}

}
