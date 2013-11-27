package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupAddPerm extends Command {

	public CChannelGroupAddPerm(int groupId, String permName, int permValue) {
		super("channelgroupaddperm");
		add(new KeyValueParam("cgid", groupId));
		add(new KeyValueParam("permsid", permName));
		add(new KeyValueParam("permvalue", permValue));
	}

}
