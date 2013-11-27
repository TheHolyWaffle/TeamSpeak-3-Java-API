package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.TS3Query;

public class DefaultListener implements TS3Listener {

	public void onTextMessage(TextMessageEvent e) {
		TS3Query.log("[Text Message] " + e.getInvokerName() + ": "
				+ e.getMessage(), true);
	}

	public void onClientJoin(ClientJoinEvent e) {
		TS3Query.log("[Player Joined] " + e.getClientNickname() + " from:"
				+ e.getClientCountry() +" id:"+e.getClientId()+" type:"+e.getClientType(), true);
	}

	public void onClientLeave(ClientLeaveEvent e) {
		TS3Query.log("[Player Left] id: " + e.getClientId()+ "from:"+e.getClientFromId() +" to:"+e.getClientTargetId()+" name:"+e.getInvokerName(), true);
	}

	public void onServerEdit(ServerEditedEvent e) {
		TS3Query.log("[Server Edited] by " + e.getInvokerName(), true);

	}

	public void onChannelEdit(ChannelEditedEvent e) {
		TS3Query.log("[Channel Edited] by " + e.getInvokerName(), true);

	}

	public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
		TS3Query.log("[Ch. Dscr. Changed] id: " + e.getChannelId(), true);

	}

	public void onClientMoved(ClientMovedEvent e) {
		TS3Query.log("[Client Moved] id: " + e.getClientId()+ "target-id:"+e.getClientTargetId(), true);

	}

}
