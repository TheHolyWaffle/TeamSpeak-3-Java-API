package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupRename extends Command {

	public CChannelGroupRename(int groupId, String name) {
		super("channelgrouprename");
		add(new KeyValueParam("cgid", groupId));
		add(new KeyValueParam("name", name));
	}

}
