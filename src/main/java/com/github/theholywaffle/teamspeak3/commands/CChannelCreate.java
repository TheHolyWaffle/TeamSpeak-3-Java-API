/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
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
