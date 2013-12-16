package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;

public class EventListenerExample {
	
	public static void main(String[] args) {
		new EventListenerExample();
	}

	public EventListenerExample() {
		TS3Api api = new TS3Query("77.77.77.77", TS3Query.DEFAULT_PORT,
				FloodRate.DEFAULT).connect().getApi();

		api.login("serveradmin", "serveradminpassword");
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");

		api.addTS3Listeners(new TS3Listener() {

			public void onTextMessage(TextMessageEvent e) {
				System.out.println("Text message received in "
						+ e.getTargetMode());
			}

			public void onServerEdit(ServerEditedEvent e) {
				System.out.println("Server edited by " + e.getInvokerName());
			}

			public void onClientMoved(ClientMovedEvent e) {
				System.out.println("Client has been moved " + e.getClientId());
			}

			public void onClientLeave(ClientLeaveEvent e) {
				// ...

			}

			public void onClientJoin(ClientJoinEvent e) {
				// ...

			}

			public void onChannelEdit(ChannelEditedEvent e) {
				// ...

			}

			public void onChannelDescriptionChanged(
					ChannelDescriptionEditedEvent e) {
				// ...

			}
		});

		api.registerAllEvents();
	}	

}
