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
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

class EventManager {

	private static final Logger log = LoggerFactory.getLogger(EventManager.class);
	private static final Map<String, Function<Wrapper, TS3Event>> eventByName = new HashMap<>(12);
	static {
		eventByName.put("notifytextmessage", TextMessageEvent::new);
		eventByName.put("notifycliententerview", ClientJoinEvent::new);
		eventByName.put("notifyclientleftview", ClientLeaveEvent::new);
		eventByName.put("notifyserveredited", ServerEditedEvent::new);
		eventByName.put("notifychanneledited", ChannelEditedEvent::new);
		eventByName.put("notifychanneldescriptionchanged", ChannelDescriptionEditedEvent::new);
		eventByName.put("notifyclientmoved", ClientMovedEvent::new);
		eventByName.put("notifychannelcreated", ChannelCreateEvent::new);
		eventByName.put("notifychanneldeleted", ChannelDeletedEvent::new);
		eventByName.put("notifychannelmoved", ChannelMovedEvent::new);
		eventByName.put("notifychannelpasswordchanged", ChannelPasswordChangedEvent::new);
		eventByName.put("notifytokenused", PrivilegeKeyUsedEvent::new);
	}

	// CopyOnWriteArrayList for thread safety
	private final Collection<ListenerTask> tasks = new CopyOnWriteArrayList<>();
	private final TS3Query ts3;

	EventManager(TS3Query query) {
		ts3 = query;
	}

	void addListeners(TS3Listener... listeners) {
		for (TS3Listener listener : listeners) {
			if (listener == null) throw new IllegalArgumentException("A listener was null");
			ListenerTask task = new ListenerTask(listener);
			tasks.add(task);
		}
	}

	void removeListeners(TS3Listener... listeners) {
		// Bad performance (O(n*m)), but this method is rarely if ever used
		List<TS3Listener> listenersToRemove = Arrays.asList(listeners);
		tasks.removeIf(listenerTask -> listenersToRemove.contains(listenerTask.listener));
	}

	void fireEvent(String notifyName, String notifyBody) {
		final DefaultArrayResponse response = DefaultArrayResponse.parse(notifyBody);

		for (Wrapper eventData : response.getResponses()) {
			TS3Event event = createEvent(notifyName, eventData);
			fireEvent(event);
		}
	}

	void fireEvent(TS3Event event) {
		if (event == null) throw new IllegalArgumentException("TS3Event was null");
		for (ListenerTask task : tasks) {
			task.enqueueEvent(event);
		}
	}

	private static TS3Event createEvent(String notifyName, Wrapper eventData) {
		Function<Wrapper, TS3Event> constructor = eventByName.get(notifyName);
		if (constructor == null) throw new TS3UnknownEventException(notifyName + " " + eventData);
		return constructor.apply(eventData);
	}

	/*
	 * Do not synchronize on instances of this class from outside the class itself!
	 */
	private class ListenerTask implements Runnable {

		private static final int START_QUEUE_SIZE = 16;

		private final TS3Listener listener;
		private final Queue<TS3Event> eventQueue;

		ListenerTask(TS3Listener ts3Listener) {
			listener = ts3Listener;
			eventQueue = new ArrayDeque<>(START_QUEUE_SIZE);
		}

		TS3Listener getListener() {
			return listener;
		}

		synchronized void enqueueEvent(TS3Event event) {
			if (eventQueue.isEmpty()) {
				// Add the event to the queue and start a task to process this event and any events
				// that might be enqueued before the last event is removed from the queue
				eventQueue.add(event);
				ts3.submitUserTask("Event listener task", this);
			} else {
				// Just add the event to the queue, the running task will pick it up
				eventQueue.add(event);
			}
		}

		@Override
		public void run() {
			TS3Event currentEvent;
			synchronized (this) {
				currentEvent = eventQueue.peek();
				if (currentEvent == null) throw new IllegalStateException("Task started without events");
			}

			do {
				try {
					currentEvent.fire(listener);
				} catch (Throwable throwable) {
					log.error("Event listener threw an exception", throwable);
				}

				synchronized (this) {
					eventQueue.remove();
					currentEvent = eventQueue.peek();
				}
			} while (currentEvent != null);
		}
	}
}
