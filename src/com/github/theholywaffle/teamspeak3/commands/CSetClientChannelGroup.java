package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CSetClientChannelGroup extends Command{

	public CSetClientChannelGroup(int groupId, int channelId, int clientDBId) {
		super("setclientchannelgroup");
		add(new KeyValueParam("cgid",groupId+""));
		add(new KeyValueParam("cid",channelId+""));
		add(new KeyValueParam("cldbid",clientDBId+""));
	}

}
