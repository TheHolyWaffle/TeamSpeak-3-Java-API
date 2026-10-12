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

import java.util.Objects;

/**
 * A permission assigned to a server group, channel group, channel or client.
 * <p>
 * The value is the assigned value, not necessarily the client's effective value, which other
 * assignments may override. Boolean permissions use {@code 0} and {@code 1}. Negated assignments
 * select the lowest instead of the highest value. Skipped assignments (server groups and clients only)
 * are not overridden by channel group or, for server groups, client permissions.
 * </p>
 *
 * @param name the permission name; {@code b_} for boolean and {@code i_} for integer permissions
 * @param value the assigned value
 * @param negated whether the assignment is negated
 * @param skipped whether the assignment is skipped
 */
public record Permission(String name, int value, boolean negated, boolean skipped) {

	public Permission {
		Objects.requireNonNull(name, "name");
	}

	/** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if a property is missing or malformed */
	public static Permission from(Wrapper row) {
		return new Permission(row.require("permsid"), row.requireInt("permvalue"),
			row.requireBoolean("permnegated"), row.requireBoolean("permskip"));
	}
}
