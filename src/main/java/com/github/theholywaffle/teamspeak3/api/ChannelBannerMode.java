package com.github.theholywaffle.teamspeak3.api;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2021 Bert De Geyter, Roger Baumgartner
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

/**
 * The values in this enum are <b>speculative</b>.
 * There does not appear to be any documentation on channel banner modes.
 */
public enum ChannelBannerMode {

	NO_ADJUST(0),
	IGNORE_ASPECT(1),
	KEEP_ASPECT(2),

	/**
	 * An unknown channel banner mode.
	 * <p>
	 * If you ever encounter an unknown banner mode, please tell us!
	 * We may have to update the API.
	 * </p>
	 */
	UNKNOWN(-1);

	private final int i;

	ChannelBannerMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}
}
