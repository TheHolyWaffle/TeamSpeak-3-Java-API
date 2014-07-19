package com.github.theholywaffle.teamspeak3.api.event;

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


import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

public class ChannelMovedEvent extends Wrapper implements TS3EventEmitter,TS3Event {

	public ChannelMovedEvent() {
		super(null);
	}
	public ChannelMovedEvent(HashMap<String, String> map) {
		super(map);
	}
	public int getChannelId() {
		return getInt("cid");
	}
	public int getChannelParentId(){
		return getInt("cpid");
	}
	public int getChannelOrder(){
		return getInt("channel_order");
	}
	public int getReasonId(){
		return getInt("reasonid");
	}
	public int getInvokerId(){
		return getInt("invokerid");
	}
	public String getInvokerName(){
		return get("invokername");
	}
	public String getInvokerUId(){
		return get("invokeruid");
	}
	@Override
	public void fire(TS3Listener listener, HashMap<String, String> map) {
		listener.onChannelMoved(new ChannelMovedEvent(map));
	}

}
