package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupAdd extends Command {

	public CChannelGroupAdd(String name) {
		this(name, null);
	}

	public CChannelGroupAdd(String name, PermissionGroupDatabaseType t) {
		super("channelgroupadd");
		add(new KeyValueParam("name", name));
		if (t != null) {
			add(new KeyValueParam("type", t.getIndex()));
		}
	}

}
