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

/**
 * A template class implementing {@link TS3Listener} similar to Swing's event adapters.
 * <p>
 * All method in this class do nothing, so the user only has to override the interface
 * methods for the events they want to take action on.
 * </p>
 */
public abstract class TS3EventAdapter implements TS3Listener {

	@Override
	public void onTextMessage(TextMessageEvent e) {}

	@Override
	public void onClientJoin(ClientJoinEvent e) {}

	@Override
	public void onClientLeave(ClientLeaveEvent e) {}

	@Override
	public void onServerEdit(ServerEditedEvent e) {}

	@Override
	public void onChannelEdit(ChannelEditedEvent e) {}

	@Override
	public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}

	@Override
	public void onClientMoved(ClientMovedEvent e) {}

	@Override
	public void onChannelCreate(ChannelCreateEvent e) {}

	@Override
	public void onChannelDeleted(ChannelDeletedEvent e) {}

	@Override
	public void onChannelMoved(ChannelMovedEvent e) {}

	@Override
	public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}
}
