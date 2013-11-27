package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelDelPerm extends Command {

	public CChannelDelPerm(int channelId, String permName) {
		super("channeldelperm");
		add(new KeyValueParam("cid",channelId));
		add(new KeyValueParam("permsid",permName));
	}

}
