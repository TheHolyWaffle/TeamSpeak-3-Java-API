package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelGroupClientList extends Command {

	public CChannelGroupClientList(int channelId, int clientDBId, int groupId) {
		super("channelgroupclientlist");
		if (channelId > 0) {
			add(new KeyValueParam("cid", channelId));
		}
		if (clientDBId > 0) {
			add(new KeyValueParam("cldbid", clientDBId));
		}
		if (groupId > 0) {
			add(new KeyValueParam("cgid", groupId));
		}
	}

}
