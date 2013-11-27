package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CChannelClientPermList extends Command {

	public CChannelClientPermList(int channelId, int clientDBId) {
		super("channelclientpermlist");
		add(new KeyValueParam("cid",channelId));
		add(new KeyValueParam("cldbid",clientDBId));
		add(new OptionParam("permsid"));
	}

	

}
