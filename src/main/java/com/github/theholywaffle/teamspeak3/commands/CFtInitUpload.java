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

public class CFtInitUpload extends Command {

	public CFtInitUpload(int transferId, String path, int channelId, String channelPassword, long size, boolean overwrite) {
		super("ftinitupload");
		add(new KeyValueParam("clientftfid", transferId));
		add(new KeyValueParam("name", path.startsWith("/") ? path : "/" + path));
		add(new KeyValueParam("cid", channelId));
		add(new KeyValueParam("cpw", channelPassword != null ? channelPassword : ""));
		add(new KeyValueParam("size", size));
		add(new KeyValueParam("overwrite", overwrite));
		add(new KeyValueParam("resume", 0));
		add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
	}
}
