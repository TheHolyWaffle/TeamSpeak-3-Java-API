package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
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
		final TS3Api api = new TS3Query("77.77.77.77", TS3Query.DEFAULT_PORT,
				FloodRate.DEFAULT).connect().getApi();

		api.login("serveradmin", "serveradminpassword");
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		
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
		});
	}

	

}
