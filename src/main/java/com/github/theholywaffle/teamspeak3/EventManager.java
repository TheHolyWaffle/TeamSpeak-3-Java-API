package com.github.theholywaffle.teamspeak3;

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

import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnknownEventException;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventManager {

	private final List<TS3Listener> listeners = new LinkedList<>();

	public void addListeners(TS3Listener... listeners) {
		for (final TS3Listener l : listeners) {
			this.listeners.add(l);
		}
	}

	public void removeListeners(TS3Listener... listeners) {
		for (final TS3Listener l : listeners) {
			this.listeners.remove(l);
		}
	}

	public void fireEvent(String notifyName, String notifyBody) {
		TS3Event event = createEvent(notifyName, notifyBody);

		for (final TS3Listener listener : listeners) {
			event.fire(listener);
		}
	}

	private TS3Event createEvent(String notifyName, String notifyBody) {
		final DefaultArrayResponse response = new DefaultArrayResponse(notifyBody);
		final Map<String, String> eventData = response.getFirstResponse().getMap();

		switch (notifyName) {
			case "notifytextmessage":
				return new TextMessageEvent(eventData);
			case "notifycliententerview":
				return new ClientJoinEvent(eventData);
			case "notifyclientleftview":
				return new ClientLeaveEvent(eventData);
			case "notifyserveredited":
				return new ServerEditedEvent(eventData);
			case "notifychanneledited":
				return new ChannelEditedEvent(eventData);
			case "notifychanneldescriptionchanged":
				return new ChannelDescriptionEditedEvent(eventData);
			case "notifyclientmoved":
				return new ClientMovedEvent(eventData);
			case "notifychannelcreated":
				return new ChannelCreateEvent(eventData);
			case "notifychanneldeleted":
				return new ChannelDeletedEvent(eventData);
			case "notifychannelmoved":
				return new ChannelMovedEvent(eventData);
			case "notifychannelpasswordchanged":
				return new ChannelPasswordChangedEvent(eventData);
			case "notifytokenused":
				return new PrivilegeKeyUsedEvent(eventData);
			default:
				throw new TS3UnknownEventException(notifyName + " " + notifyBody);
		}
	}
}
