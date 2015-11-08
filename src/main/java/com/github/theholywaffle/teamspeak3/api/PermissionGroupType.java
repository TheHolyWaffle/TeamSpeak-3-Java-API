package com.github.theholywaffle.teamspeak3.api;

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

import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionAssignment;

/**
 * An enum describing where a {@link PermissionAssignment} originates from.
 */
public enum PermissionGroupType {

	/**
	 * A permission assigned to a server group.
	 */
	SERVER_GROUP(0),

	/**
	 * A permission assigned to a client on a server-wide level.
	 */
	GLOBAL_CLIENT(1),

	/**
	 * A permission requirement for certain actions in a channel.
	 * <p>
	 * Examples: Join, talk, subscribe and file upload permission requirements
	 * </p>
	 */
	CHANNEL(2),

	/**
	 * A permission assigned to a channel group.
	 */
	CHANNEL_GROUP(3),

	/**
	 * A permission assigned to a client in a specific channel.
	 * <p>
	 * This is mostly used for the priority speaker feature by granting
	 * {@code b_client_is_priority_speaker} to a client in a channel.
	 * </p>
	 */
	CHANNEL_CLIENT(4);

	private final int i;

	PermissionGroupType(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
