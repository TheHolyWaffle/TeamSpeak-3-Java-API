package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelInfo extends Command {

	public CChannelInfo(int channelId) {
		super("channelinfo");
		add(new KeyValueParam("cid", channelId));
	}

}
