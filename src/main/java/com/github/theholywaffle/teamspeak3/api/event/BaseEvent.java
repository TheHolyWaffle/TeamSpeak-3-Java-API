package com.github.theholywaffle.teamspeak3.api.event;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 - 2015 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

import java.util.Collections;

public abstract class BaseEvent extends Wrapper implements TS3Event {

	protected BaseEvent(Wrapper wrapper) {
		super(Collections.unmodifiableMap(wrapper.getMap()));
	}

	/**
	 * Gets the ID of the client who caused this event to happen.
	 * <p>
	 * Because not all events are caused by clients, not all events have an invoker.<br>
	 * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
	 * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
	 * quit or changed channels themselves. These values are only present if another client
	 * caused the event to happen by moving, kicking or banning a client.
	 * </p><p>
	 * If the event response does not contain an invoker, this method will return {@code -1}.
	 * </p>
	 *
	 * @return the client ID of the invoker or {@code -1} if the event does not contain an invoker
	 */
	public int getInvokerId() {
		return getInt("invokerid");
	}

	/**
	 * Gets the nickname of the client who caused this event to happen.
	 * <p>
	 * Because not all events are caused by clients, not all events have an invoker.<br>
	 * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
	 * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
	 * quit or changed channels themselves. These values are only present if another client
	 * caused the event to happen by moving, kicking or banning a client.
	 * </p><p>
	 * If the event response does not contain an invoker, this method will return an empty string.
	 * </p>
	 *
	 * @return the nickname of the invoker or an empty string if the event does not contain an invoker
	 */
	public String getInvokerName() {
		return get("invokername");
	}

	/**
	 * Gets the unique identifier of the client who caused this event to happen.
	 * <p>
	 * Because not all events are caused by clients, not all events have an invoker.<br>
	 * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
	 * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
	 * quit or changed channels themselves. These values are only present if another client
	 * caused the event to happen by moving, kicking or banning a client.
	 * </p><p>
	 * If the event response does not contain an invoker, this method will return an empty string.
	 * </p>
	 *
	 * @return the unique ID the invoker or an empty string if the event does not contain an invoker
	 */
	public String getInvokerUniqueId() {
		return get("invokeruid");
	}

	/**
	 * Gets the reason ID of the cause of this event.
	 * <p>
	 * Here's an incomplete list of known reason IDs:
	 * </p>
	 * <table>
	 * <caption>Incomplete list of known reason IDs</caption>
	 * <tr><th>ID</th><th>Description</th></tr>
	 * <tr><td>-1</td><td>No reason present</td></tr>
	 * <tr><td>0</td><td>No external cause</td></tr>
	 * <tr><td>1</td><td>Client was moved (by other client / by creating a new channel)</td></tr>
	 * <tr><td>3</td><td>Client timed out</td></tr>
	 * <tr><td>4</td><td>Client kicked from a channel or channel deleted</td></tr>
	 * <tr><td>5</td><td>Client kicked from the server</td></tr>
	 * <tr><td>6</td><td>Client banned from the server</td></tr>
	 * <tr><td>8</td><td>Client left the server (no external cause)</td></tr>
	 * <tr><td>10</td><td>Virtual server or channel edited</td></tr>
	 * <tr><td>11</td><td>Server shut down</td></tr>
	 * </table>
	 *
	 * @return the reason ID for this event
	 */
	public int getReasonId() {
		return getInt("reasonid");
	}

	/**
	 * Gets the reason for this event.
	 * This field is rarely present, even if the reason ID is non-zero.
	 * <p>
	 * In case of a client getting kicked or banned, this will return the user-provided reason.
	 * </p>
	 *
	 * @return the reason for this event or an empty string if no reason message is present
	 */
	public String getReasonMessage() {
		return get("reasonmsg");
	}
}
