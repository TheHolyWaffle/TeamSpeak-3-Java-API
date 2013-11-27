package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupDelPerm extends Command{

	public CChannelGroupDelPerm(int groupId, String permName) {
		super("channelgroupdelperm");
		add(new KeyValueParam("cgid",groupId+""));
		add(new KeyValueParam("permsid",permName));
	}

}
