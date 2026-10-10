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
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
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

	private final CopyOnWriteArrayList<ListenerTask> tasks = new CopyOnWriteArrayList<>();
	private final TS3Query ts3;
	private final ThreadPoolExecutor executor;
	private final AtomicLong dropped = new AtomicLong();
	private final AtomicLong unknown = new AtomicLong();
	private final AtomicLong malformed = new AtomicLong();
	private final AtomicLong failures = new AtomicLong();
	private final AtomicLong rejected = new AtomicLong();
	private boolean closed;

	EventManager(TS3Query query) {
		ts3 = query;
		int threads = query.getConfig().getEventCallbackThreads();
		executor = new ThreadPoolExecutor(threads, threads, 0,
			TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(query.getConfig().getListenerCapacity()),
			Thread.ofPlatform().name("[TeamSpeak-3-Java-API] Event-", 0).factory(),
			new ThreadPoolExecutor.AbortPolicy());
	}

	synchronized void addListeners(TS3Listener... listeners) {
		checkCapacity(listeners.length);
		for (TS3Listener listener : listeners) Objects.requireNonNull(listener, "listener");
		for (TS3Listener listener : listeners) tasks.add(new ListenerTask(listener));
	}

	synchronized EventSubscription subscribe(TS3Listener listener) {
		Objects.requireNonNull(listener, "listener");
		checkCapacity(1);
		ListenerTask task = new ListenerTask(listener);
		tasks.add(task);
		return () -> remove(task);
	}

	private void checkCapacity(int added) {
		if (closed) throw new IllegalStateException("Query event subscriptions are closed");
		if (added > ts3.getConfig().getListenerCapacity() - tasks.size()) {
			throw new IllegalStateException("Listener capacity exceeded");
		}
	}

	private synchronized void remove(ListenerTask task) {
		task.close();
		tasks.remove(task);
	}

	synchronized void removeListeners(TS3Listener... listeners) {
		List<TS3Listener> toRemove = Arrays.asList(listeners);
		for (ListenerTask task : tasks) if (toRemove.contains(task.listener)) remove(task);
	}

	void fireEvent(String notifyName, String notifyBody) {
		Function<Wrapper, TS3Event> constructor = eventByName.get(notifyName);
		if (constructor == null) {
			unknown.incrementAndGet();
			log.debug("Ignoring unsupported notification type: {}", notifyName);
			return;
		}
		if (notifyBody == null || notifyBody.isBlank()) { malformedNotification(); return; }
		try {
			for (Wrapper data : DefaultArrayResponse.parse(notifyBody).getResponses()) {
				fireEvent(constructor.apply(data));
			}
		} catch (RuntimeException failure) {
			malformedNotification();
		}
	}

	void malformedNotification() {
		malformed.incrementAndGet();
		log.debug("Ignoring malformed notification");
	}

	EventStatistics statistics() {
		return new EventStatistics(dropped.get(), unknown.get(), malformed.get(), failures.get(), rejected.get());
	}

	void fireEvent(TS3Event event) {
		Objects.requireNonNull(event, "event");
		for (ListenerTask task : tasks) task.enqueueEvent(event);
	}

	synchronized void close() {
		closed = true;
		for (ListenerTask task : tasks) task.close();
		tasks.clear();
		executor.shutdownNow();
	}

	void awaitTermination(Deadline deadline) {
		try { executor.awaitTermination(deadline.remaining(), TimeUnit.NANOSECONDS); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	boolean isTerminated() { return executor.isTerminated(); }

	private final class ListenerTask implements Runnable {
		private final TS3Listener listener;
		private final Queue<TS3Event> events = new ArrayDeque<>();
		private boolean running;
		private boolean removed;

		ListenerTask(TS3Listener listener) { this.listener = listener; }

		synchronized void close() {
			removed = true;
			events.clear();
			executor.remove(this);
		}

		synchronized void enqueueEvent(TS3Event event) {
			if (removed) return;
			if (events.size() >= ts3.getConfig().getListenerQueueCapacity()) {
				dropped.incrementAndGet();
				return; // Drop newest; reader never waits for callbacks.
			}
			events.add(event);
			if (running) return;
			running = true;
			try { executor.execute(this); }
			catch (RejectedExecutionException failure) {
				rejected.incrementAndGet();
				dropped.addAndGet(events.size());
				events.clear();
				running = false;
			}
		}

		@Override public void run() {
			while (true) {
				TS3Event event;
				synchronized (this) {
					if (removed || (event = events.poll()) == null) { running = false; return; }
				}
				try { ts3.runUserTask(() -> event.fire(listener)); }
				catch (Throwable failure) {
					failures.incrementAndGet();
					log.error("Event listener threw an exception", failure);
				}
			}
		}
	}
}
