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

import java.util.Map;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;

public class Channel extends ChannelBase {

	public Channel(Map<String, String> map) {
		super(map);
	}

	@Override
	public int getId() {
		return getInt(ChannelProperty.CID);
	}

	public int getTotalClientsFamily() {
		return getInt("total_clients_family");
	}

	public int getTotalClients() {
		return getInt("total_clients");
	}

	public int getNeededSubscribePower() {
		return getInt(ChannelProperty.CHANNEL_NEEDED_SUBSCRIBE_POWER);
	}

	/**
	 * @return {@code true}, if the channel is empty, {@code false} otherwise.
	 */
	public boolean isEmpty() {
		return (getTotalClients() == 0);
	}

	@Override
	public boolean isFamilyEmpty() {
		return (getTotalClientsFamily() == 0);
	}
}
