package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelAddPerm extends Command {

	public CChannelAddPerm(int channelid, String permName, int permValue) {
		super("channeladdperm");
		add(new KeyValueParam("cid", channelid));
		add(new KeyValueParam("permsid", permName));
		add(new KeyValueParam("permvalue", permValue));
	}

}
