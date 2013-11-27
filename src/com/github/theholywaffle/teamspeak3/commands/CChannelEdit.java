package com.github.theholywaffle.teamspeak3.commands;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CChannelEdit extends Command {

	public CChannelEdit(int channelId, HashMap<ChannelProperty, String> options) {
		super("channeledit");
		add(new KeyValueParam("cid",channelId));
		if (options != null) {
			for (ChannelProperty p : options.keySet()) {
				if (p.isChangeable()) {
					add(new KeyValueParam(p.getName(), options.get(p)));
				}
			}
		}
	}

}
