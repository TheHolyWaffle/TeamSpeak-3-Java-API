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

import java.util.Locale;

public enum ChannelProperty implements Property {

	CHANNEL_CODEC(true),
	CHANNEL_CODEC_IS_UNENCRYPTED(true),
	CHANNEL_CODEC_LATENCY_FACTOR(false),
	CHANNEL_CODEC_QUALITY(true),
	CHANNEL_DELETE_DELAY(true),
	CHANNEL_DESCRIPTION(true),
	CHANNEL_FILEPATH(false),
	CHANNEL_FLAG_DEFAULT(true),
	CHANNEL_FLAG_MAXCLIENTS_UNLIMITED(true),
	CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED(true),
	CHANNEL_FLAG_MAXFAMILYCLIENTS_UNLIMITED(true),
	CHANNEL_FLAG_PASSWORD(false),
	CHANNEL_FLAG_PERMANENT(true),
	CHANNEL_FLAG_SEMI_PERMANENT(true),
	CHANNEL_FLAG_TEMPORARY(true),
	CHANNEL_FORCED_SILENCE(false),
	CHANNEL_ICON_ID(true),
	CHANNEL_MAXCLIENTS(true),
	CHANNEL_MAXFAMILYCLIENTS(true),
	CHANNEL_NAME(true),
	CHANNEL_NAME_PHONETIC(true),
	CHANNEL_NEEDED_SUBSCRIBE_POWER(false),
	CHANNEL_NEEDED_TALK_POWER(true),
	CHANNEL_ORDER(true),
	CHANNEL_PASSWORD(true),
	CHANNEL_TOPIC(true),
	SECONDS_EMPTY(false),
	CID(false),
	PID(false),
	CPID(true);

	private final boolean changeable;

	ChannelProperty(boolean changeable) {
		this.changeable = changeable;
	}

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean isChangeable() {
		return changeable;
	}

}
