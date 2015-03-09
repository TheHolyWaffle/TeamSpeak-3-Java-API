package com.github.theholywaffle.teamspeak3.api.event;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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

	public void onChannelCreate(ChannelCreateEvent e) {

	}

	public void onChannelDeleted(ChannelDeletedEvent e) {

	}

	public void onChannelMoved(ChannelMovedEvent e) {

	}

	public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {

	}

}
