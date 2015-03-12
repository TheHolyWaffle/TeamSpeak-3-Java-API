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

import java.util.Date;
import java.util.Map;

public class Ban extends Wrapper {

	public Ban(Map<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt("banid");
	}

	public String getBannedIp() {
		return get("ip");
	}

	public String getBannedName() {
		return get("name");
	}

	public String getBannedUId() {
		return get("uid");
	}

	public String getLastNickname() {
		return get("lastnickname");
	}

	public Date getCreatedDate() {
		return new Date(getLong("created") * 1000);
	}

	public long getDuration() {
		return getLong("duration");
	}

	public String getInvokerName() {
		return get("invokername");
	}

	public int getInvokerClientDBId() {
		return getInt("invokercldbid");
	}

	public String getInvokerUId() {
		return get("invokeruid");
	}

	public String getReason() {
		return get("reason");
	}

	public int getEnforcements() {
		return getInt("enforcements");
	}
}
