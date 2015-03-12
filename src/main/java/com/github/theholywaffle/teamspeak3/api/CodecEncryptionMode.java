package com.github.theholywaffle.teamspeak3.api;

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

import com.github.theholywaffle.teamspeak3.TS3Api;

import java.util.Map;

/**
 * Describes how the virtual server manages audio encryption.
 * <p>
 * Can be edited with {@link TS3Api#editServer(Map)}.
 * </p>
 */
public enum CodecEncryptionMode {

	/**
	 * Each channel manages audio encryption on its own.
	 *
	 * @see TS3Api#editChannel(int, Map)
	 */
	CODEC_CRYPT_INDIVIDUAL(0),

	/**
	 * Audio encryption is globally disabled on this virtual server.
	 */
	CODEC_CRYPT_DISABLED(1),

	/**
	 * Audio encryption is globally enabled on this virtual server.
	 */
	CODEC_CRYPT_ENABLED(2),

	/**
	 * An unknown codec encryption mode.
	 * <p>
	 * If you ever encounter an unknown mode, please tell us!
	 * We may have to update the API.
	 * </p>
	 */
	UNKNOWN(-1);

	private final int i;

	CodecEncryptionMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}
}
