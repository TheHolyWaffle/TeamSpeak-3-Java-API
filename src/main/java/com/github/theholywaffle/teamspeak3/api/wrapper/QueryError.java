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

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class QueryError extends Wrapper {

	private static final int ERROR_ID_OK = 0;
	private static final int ERROR_ID_EMPTY_RESULT_SET = 1281;

	public QueryError(Map<String, String> map) {
		super(map);
	}

	public int getId() {
		return requireInt("id");
	}

	public String getMessage() {
		return require("msg");
	}

	public Optional<String> getExtraMessage() {
		return findText("extra_msg");
	}

	public OptionalInt getFailedPermissionId() {
		return findInt("failed_permid");
	}

	public boolean isSuccessful() {
		return isSuccessful(getId());
	}

	/**
	 * Checks whether an error ID reports success: {@code 0} (ok) or {@code 1281} (empty result set).
	 *
	 * @param id the error ID
	 * @return whether the ID reports success
	 */
	public static boolean isSuccessful(int id) {
		return id == ERROR_ID_OK || id == ERROR_ID_EMPTY_RESULT_SET;
	}
}
