package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelDelete extends Command {

	public CChannelDelete(int channelId, boolean forced) {
		super("channeldelete");
		add(new KeyValueParam("cid", channelId));
		add(new KeyValueParam("force", forced ? "1" : "0"));
	}

}
