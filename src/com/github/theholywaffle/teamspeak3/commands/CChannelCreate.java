package com.github.theholywaffle.teamspeak3.commands;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelCreate extends Command {

	public CChannelCreate(String name, HashMap<ChannelProperty, String> options) {
		super("channelcreate");
		add(new KeyValueParam("channel_name", name));
		if (options != null) {
			for (ChannelProperty p : options.keySet()) {
				if (p.isChangeable()) {
					add(new KeyValueParam(p.getName(), options.get(p)));
				}
			}
		}
	}

}
