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

import com.github.theholywaffle.teamspeak3.api.ChannelBannerMode;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChannelInfo extends ChannelBase {

	private final int channelId;

	public ChannelInfo(int channelId, Map<String, String> map) {
		super(map);
		this.channelId = channelId;
	}

	@Override
	public int getId() {
		return channelId;
	}

	public String getDescription() {
		return require(ChannelProperty.CHANNEL_DESCRIPTION);
	}

	/** @return the hashed channel password, or empty if the channel has no password */
	public Optional<String> getPassword() {
		return findText(ChannelProperty.CHANNEL_PASSWORD);
	}

	/** @return the banner mode, or {@link ChannelBannerMode#UNKNOWN} for a mode this library does not know */
	public ChannelBannerMode getBannerMode() {
		return requireCode(ChannelProperty.CHANNEL_BANNER_MODE, ChannelBannerMode::fromIndex);
	}

	/** @return the banner image URL, or empty if none is set */
	public Optional<String> getBannerGraphicsUrl() {
		return findText(ChannelProperty.CHANNEL_BANNER_GFX_URL);
	}

	public int getCodecLatencyFactor() {
		return requireInt(ChannelProperty.CHANNEL_CODEC_LATENCY_FACTOR);
	}

	public UUID getUniqueIdentifier() {
		return requireParsed(ChannelProperty.CHANNEL_UNIQUE_IDENTIFIER.getName(), "a UUID", UUID::fromString);
	}

	public boolean isEncrypted() {
		return !requireBoolean(ChannelProperty.CHANNEL_CODEC_IS_UNENCRYPTED);
	}

	public boolean hasUnlimitedClients() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED);
	}

	public boolean hasUnlimitedFamilyClients() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_UNLIMITED);
	}

	public boolean hasInheritedMaxFamilyClients() {
		return requireBoolean(ChannelProperty.CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED);
	}

	public String getFilePath() {
		return require(ChannelProperty.CHANNEL_FILEPATH);
	}

	public boolean isForcedSilence() {
		return requireBoolean(ChannelProperty.CHANNEL_FORCED_SILENCE);
	}

	public String getPhoneticName() {
		return require(ChannelProperty.CHANNEL_NAME_PHONETIC);
	}

	@Override
	public boolean isFamilyEmpty() {
		return getTimeEmpty().isPresent();
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other) && channelId == ((ChannelInfo) other).channelId;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + channelId;
	}
}
