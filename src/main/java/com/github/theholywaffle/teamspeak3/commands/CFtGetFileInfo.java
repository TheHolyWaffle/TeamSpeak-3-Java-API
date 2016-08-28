package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2016 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CFtGetFileInfo extends Command {

	public CFtGetFileInfo(int channelId, String channelPassword, String... filePaths) {
		super("ftgetfileinfo");

		final ArrayParameter p = new ArrayParameter(filePaths.length, 3);
		for (String filePath : filePaths) {
			p.add(new KeyValueParam("cid", channelId));
			p.add(new KeyValueParam("cpw", channelPassword != null ? channelPassword : ""));
			p.add(new KeyValueParam("name", filePath));
		}
		add(p);
	}

	public CFtGetFileInfo(int[] channelIds, String[] channelPasswords, String[] filePaths) {
		super("ftgetfileinfo");

		if (channelIds.length != filePaths.length) {
			throw new IllegalArgumentException("Channel IDs length doesn't match file paths length");
		}
		if (channelPasswords != null && filePaths.length != channelPasswords.length) {
			throw new IllegalArgumentException("Passwords length doesn't match file paths length");
		}

		final ArrayParameter p = new ArrayParameter(filePaths.length, 3);
		for (int i = 0; i < filePaths.length; ++i) {
			final String password = channelPasswords == null ? null : channelPasswords[i];
			final String path = filePaths[i];

			p.add(new KeyValueParam("cid", channelIds[i]));
			p.add(new KeyValueParam("cpw", password != null ? password : ""));
			p.add(new KeyValueParam("name", path.startsWith("/") ? path : "/" + path));
		}
		add(p);
	}
}
