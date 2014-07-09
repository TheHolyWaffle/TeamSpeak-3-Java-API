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
package com.github.theholywaffle.teamspeak3.api.event;

public class DefaultListener implements TS3Listener {

	public void onTextMessage(TextMessageEvent e) {
		System.out.println("[Text Message] " + e.getInvokerName() + ": "
				+ e.getMessage());
	}

	public void onClientJoin(ClientJoinEvent e) {
		System.out.println("[Player Joined] " + e.getClientNickname()
				+ " from:" + e.getClientCountry() + " id:" + e.getClientId()
				+ " type:" + e.getClientType());
	}

	public void onClientLeave(ClientLeaveEvent e) {
		System.out.println("[Player Left] id: " + e.getClientId() + "from:"
				+ e.getClientFromId() + " to:" + e.getClientTargetId()
				+ " name:" + e.getInvokerName());
	}

	public void onServerEdit(ServerEditedEvent e) {
		System.out.println("[Server Edited] by " + e.getInvokerName());

	}

	public void onChannelEdit(ChannelEditedEvent e) {
		System.out.println("[Channel Edited] by " + e.getInvokerName());

	}

	public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
		System.out.println("[Ch. Dscr. Changed] id: " + e.getChannelId());

	}

	public void onClientMoved(ClientMovedEvent e) {
		System.out.println("[Client Moved] id: " + e.getClientId()
				+ "target-id:" + e.getClientTargetId());

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

}
