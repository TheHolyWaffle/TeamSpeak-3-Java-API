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

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;

import java.util.Optional;

public class TextMessageEvent extends BaseEvent {

	public TextMessageEvent(Wrapper wrapper) {
		super(wrapper);
	}

	/** @return the target mode, or {@link TextMessageTargetMode#UNKNOWN} for a mode this library does not know */
	public TextMessageTargetMode getTargetMode() {
		return requireCode("targetmode", TextMessageTargetMode::fromIndex);
	}

	public String getMessage() {
		return require("msg");
	}

	/**
	 * Gets the client ID of the recipient of a private message.
	 * <ul>
	 * <li>If the private message was sent <b>to</b> the query, the target ID will be the query's client ID</li>
	 * <li>If the private message was sent <b>by</b> the query, the target ID will be the recipient's client ID</li>
	 * <li>If this is not an event for a private message, this method returns an empty optional</li>
	 * </ul>
	 *
	 * @return the client ID of the recipient
	 */
	public Optional<ClientId> getTargetClientId() {
		return find("target").isEmpty() ? Optional.empty() : Optional.of(requireClientId("target"));
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onTextMessage(this);
	}
}
