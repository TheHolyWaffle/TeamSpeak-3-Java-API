package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;


public class CChannelPermList extends Command {

	public CChannelPermList(int channelId) {
		super("channelpermlist");
		add(new KeyValueParam("cid",channelId+""));
		add(new OptionParam("permsid"));
	}

	



}
