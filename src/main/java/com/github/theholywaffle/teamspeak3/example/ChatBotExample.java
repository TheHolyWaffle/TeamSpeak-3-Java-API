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
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;

public class ChatBotExample {
	
	public static void main(String[] args) {
		new ChatBotExample();
	}

	public ChatBotExample() {
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
		
		api.registerAllEvents();
		api.addTS3Listeners(new TS3Listener() {
			
			public void onTextMessage(TextMessageEvent e) {
				if(e.getTargetMode()== TextMessageTargetMode.CHANNEL){//Only react to channel messages
					if(e.getMessage().equals("!ping")){
						api.sendChannelMessage("pong");
					}
					if(e.getMessage().toLowerCase().contains("hello")){
						api.sendChannelMessage("Hello "+e.getInvokerName());
					}
				}
			}
			
			public void onServerEdit(ServerEditedEvent e) {
								
			}
			
			public void onClientMoved(ClientMovedEvent e) {
								
			}
			
			public void onClientLeave(ClientLeaveEvent e) {
								
			}
			
			public void onClientJoin(ClientJoinEvent e) {
								
			}
			
			public void onChannelEdit(ChannelEditedEvent e) {
								
			}
			
			public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
								
			}

			@Override
			public void onChannelCreate(ChannelCreateEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChannelDeleted(ChannelDeletedEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChannelMoved(ChannelMovedEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	

}
