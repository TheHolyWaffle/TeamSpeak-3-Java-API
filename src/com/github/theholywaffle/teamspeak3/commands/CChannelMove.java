package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelMove extends Command {

	public CChannelMove(int channelId, int channelParentId, int order) {
		super("channelmove");
		add(new KeyValueParam("cid", channelId + ""));
		add(new KeyValueParam("cpid", channelId + ""));
		if (order < 0) {
			order = 0;
		}
		add(new KeyValueParam("order", order + ""));

	}

}
