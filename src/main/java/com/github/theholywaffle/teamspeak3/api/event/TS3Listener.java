/*******************************************************************************
 * Copyright (c) 2013 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.api.event;

public interface TS3Listener {

	public void onTextMessage(TextMessageEvent e);

	public void onClientJoin(ClientJoinEvent e);

	public void onClientLeave(ClientLeaveEvent e);

	public void onServerEdit(ServerEditedEvent e);

	public void onChannelEdit(ChannelEditedEvent e);

	public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e);

	public void onClientMoved(ClientMovedEvent e);

}
