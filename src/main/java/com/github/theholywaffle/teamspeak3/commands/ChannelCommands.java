package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2017 Bert De Geyter, Roger Baumgartner
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
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

import java.util.Map;

public final class ChannelCommands {

	private ChannelCommands() {
		throw new Error("No instances");
	}

	public static Command channelCreate(String name, Map<ChannelProperty, String> options) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Channel name must be a non-empty string");
		}

		CommandBuilder builder = new CommandBuilder("channelcreate", 2);
		builder.add(new KeyValueParam("channel_name", name));
		builder.addProperties(options);
		return builder.build();
	}

	public static Command channelDelete(int channelId, boolean force) {
		CommandBuilder builder = new CommandBuilder("channeldelete", 2);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("force", force));
		return builder.build();
	}

	public static Command channelEdit(int channelId, Map<ChannelProperty, String> options) {
		CommandBuilder builder = new CommandBuilder("channeledit", 2);
		builder.add(new KeyValueParam("cid", channelId));
		builder.addProperties(options);
		return builder.build();
	}

	public static Command channelFind(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			throw new IllegalArgumentException("Channel name pattern must be a non-empty string");
		}

		return new CommandBuilder("channelfind", 1).add(new KeyValueParam("pattern", pattern)).build();
	}

	public static Command channelInfo(int channelId) {
		return new CommandBuilder("channelinfo", 1).add(new KeyValueParam("cid", channelId)).build();
	}

	public static Command channelList() {
		CommandBuilder builder = new CommandBuilder("channellist", 7);
		builder.add(new OptionParam("topic"));
		builder.add(new OptionParam("flags"));
		builder.add(new OptionParam("voice"));
		builder.add(new OptionParam("limits"));
		builder.add(new OptionParam("icon"));
		builder.add(new OptionParam("secondsempty"));
		builder.add(new OptionParam("banners"));
		return builder.build();
	}

	public static Command channelMove(int channelId, int channelParentId, int order) {
		CommandBuilder builder = new CommandBuilder("channelmove", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpid", channelParentId));
		builder.add(new KeyValueParam("order", order < 0 ? 0 : order));
		return builder.build();
	}
}
