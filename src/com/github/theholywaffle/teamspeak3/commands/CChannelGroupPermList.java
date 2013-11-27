package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public class CChannelGroupPermList extends Command{

	public CChannelGroupPermList(int groupId) {
		super("channelgrouppermlist");
		add(new KeyValueParam("cgid",groupId+""));
		add(new OptionParam("permsid"));
	}

}
