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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class ChannelBase extends Wrapper {

	protected ChannelBase(Map<String, String> map) {
		super(map);
	}

	public abstract int getId();

	public int getParentChannelId() {
		return requireInt(ChannelProperty.PID);
	}

	public int getOrder() {
		return requireInt(ChannelProperty.CHANNEL_ORDER);
	}

	public String getName() {
		return require(ChannelProperty.CHANNEL_NAME);
	}

	public String getTopic() {
		return require(ChannelProperty.CHANNEL_TOPIC);
	}

	public boolean isDefault() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_DEFAULT);
	}

	public boolean hasPassword() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_PASSWORD);
	}

	public boolean isPermanent() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_PERMANENT);
	}

	public boolean isSemiPermanent() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT);
	}

	/** @return the codec, or {@link Codec#UNKNOWN} for a codec this library does not know */
	public Codec getCodec() {
		return requireCode(ChannelProperty.CHANNEL_CODEC, Codec::fromIndex);
	}

	public int getCodecQuality() {
		return requireInt(ChannelProperty.CHANNEL_CODEC_QUALITY);
	}

	public int getNeededTalkPower() {
		return requireInt(ChannelProperty.CHANNEL_NEEDED_TALK_POWER);
	}

	public long getIconId() {
		return requireLong(ChannelProperty.CHANNEL_ICON_ID);
	}

	/** @return the client limit, or empty if the channel is unlimited ({@code -1}) */
	public OptionalInt getMaxClients() {
		return limit(ChannelProperty.CHANNEL_MAXCLIENTS);
	}

	/** @return the client limit of the channel family, or empty if unlimited ({@code -1}) */
	public OptionalInt getMaxFamilyClients() {
		return limit(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS);
	}

	/** @return how long the channel has been empty, or empty while clients are in it ({@code -1}) */
	public Optional<Duration> getTimeEmpty() {
		long seconds = requireLong(ChannelProperty.SECONDS_EMPTY);
		return seconds < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
	}

	private OptionalInt limit(ChannelProperty property) {
		int limit = requireInt(property);
		return limit < 0 ? OptionalInt.empty() : OptionalInt.of(limit);
	}

	/**
	 * @return {@code true}, if the channel and all child channels are empty, {@code false} otherwise.
	 */
	abstract public boolean isFamilyEmpty();
}
