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

import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public class CFtRenameFile extends Command {

	public CFtRenameFile(String oldPath, String newPath,
	                     int channelId, String channelPassword) {
		super("ftrenamefile");
		add(new KeyValueParam("cid", channelId));
		add(new KeyValueParam("cpw", channelPassword != null ? channelPassword : ""));
		add(new KeyValueParam("oldname", oldPath.startsWith("/") ? oldPath : "/" + oldPath));
		add(new KeyValueParam("newname", newPath.startsWith("/") ? newPath : "/" + newPath));
	}

	public CFtRenameFile(String oldPath, String newPath,
	                     int oldChannelId, String oldChannelPassword,
	                     int newChannelId, String newChannelPassword) {
		super("ftrenamefile");
		add(new KeyValueParam("cid", oldChannelId));
		add(new KeyValueParam("cpw", oldChannelPassword != null ? oldChannelPassword : ""));
		add(new KeyValueParam("tcid", newChannelId));
		add(new KeyValueParam("tcpw", newChannelPassword != null ? newChannelPassword : ""));
		add(new KeyValueParam("oldname", oldPath.startsWith("/") ? oldPath : "/" + oldPath));
		add(new KeyValueParam("newname", newPath.startsWith("/") ? newPath : "/" + newPath));
	}
}
