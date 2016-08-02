package com.github.theholywaffle.teamspeak3.api.wrapper;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 - 2015 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.Codec;

import java.util.Map;

public abstract class ChannelBase extends Wrapper {

	protected ChannelBase(Map<String, String> map) {
		super(map);
	}

	public abstract int getId();

	public int getParentChannelId() {
		return getInt(ChannelProperty.PID);
	}

	public int getOrder() {
		return getInt(ChannelProperty.CHANNEL_ORDER);
	}

	public String getName() {
		return get(ChannelProperty.CHANNEL_NAME);
	}

	public String getTopic() {
		return get(ChannelProperty.CHANNEL_TOPIC);
	}

	public boolean isDefault() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_DEFAULT);
	}

	public boolean hasPassword() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PASSWORD);
	}

	public boolean isPermanent() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_PERMANENT);
	}

	public boolean isSemiPermanent() {
		return getBoolean(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT);
	}

	public Codec getCodec() {
		final int codec = getInt(ChannelProperty.CHANNEL_CODEC);
		for (final Codec c : Codec.values()) {
			if (c.getIndex() == codec) {
				return c;
			}
		}
		return Codec.UNKNOWN;
	}

	public int getCodecQuality() {
		return getInt(ChannelProperty.CHANNEL_CODEC_QUALITY);
	}

	public int getNeededTalkPower() {
		return getInt(ChannelProperty.CHANNEL_NEEDED_TALK_POWER);
	}

	public long getIconId() {
		return getLong(ChannelProperty.CHANNEL_ICON_ID);
	}

	public int getMaxClients() {
		return getInt(ChannelProperty.CHANNEL_MAXCLIENTS);
	}

	public int getMaxFamilyClients() {
		return getInt(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS);
	}

	public int getSecondsEmpty() {
		return getInt(ChannelProperty.SECONDS_EMPTY);
	}

	/**
	 * @return {@code true}, if the channel and all child channels are empty, {@code false} otherwise.
	 */
	abstract public boolean isFamilyEmpty();
}
