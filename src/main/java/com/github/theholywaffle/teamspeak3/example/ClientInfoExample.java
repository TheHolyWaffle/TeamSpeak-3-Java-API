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
package com.github.theholywaffle.teamspeak3.example;

import java.util.logging.Level;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

public class ClientInfoExample {

	public static void main(String[] args) {
		new ClientInfoExample();
	}

	public ClientInfoExample() {
		final TS3Config config = new TS3Config();
		config.setHost("77.77.77.77");
		config.setDebugLevel(Level.ALL);
		config.setLoginCredentials("serveradmin", "serveradminpassword");
		
		final TS3Query query = new TS3Query(config);
		query.connect();
		
		final TS3Api api = query.getApi();
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		api.sendChannelMessage("PutPutBot is online!");

		for (Client c : api.getClients()) {
			System.out.println(c.getNickname() + " in channel: "
					+ api.getChannelInfo(c.getChannelId()).getName());
		}
	}

}
