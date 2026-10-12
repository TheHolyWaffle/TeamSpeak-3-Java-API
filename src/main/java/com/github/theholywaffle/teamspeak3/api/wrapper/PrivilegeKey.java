package com.github.theholywaffle.teamspeak3.api.wrapper;

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

import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;

import java.time.Instant;
import java.util.Map;

public class PrivilegeKey extends Wrapper {

	public PrivilegeKey(Map<String, String> map) {
		super(map);
	}

	public String getToken() {
		return require("token");
	}

	/** @return the key type, or {@link PrivilegeKeyType#UNKNOWN} for a type this library does not know */
	public PrivilegeKeyType getType() {
		return requireCode("token_type", PrivilegeKeyType::fromIndex);
	}

	public boolean isServerGroupToken() {
		return getType() == PrivilegeKeyType.SERVER_GROUP;
	}

	public boolean isChannelGroupToken() {
		return getType() == PrivilegeKeyType.CHANNEL_GROUP;
	}

	public int getGroupId() {
		return requireInt("token_id1");
	}

	public int getChannelId() {
		return requireInt("token_id2");
	}

	public Instant getCreated() {
		return requireInstant("token_created");
	}

	public String getDescription() {
		return require("token_description");
	}
}
