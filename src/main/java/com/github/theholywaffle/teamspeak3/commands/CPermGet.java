package com.github.theholywaffle.teamspeak3.commands;

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

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

/**
 * Command to look up the value of a permission for the server query.
 * <p>
 * As this command returns both the numerical ID and the name of a permission,
 * it is also useful for looking up a permission's ID based on its name
 * and vice versa.
 * </p>
 */
public class CPermGet extends Command {

	/**
	 * Looks up one or more permission values based on the permissions' names.
	 *
	 * @param permNames
	 * 		the names of the permissions
	 */
	public CPermGet(String... permNames) {
		super("permget");
		final ArrayParameter p = new ArrayParameter(permNames.length);
		for (String permName : permNames) {
			p.add(new KeyValueParam("permsid", permName));
		}
		add(p);
	}

	/**
	 * Looks up one or more permission values based on the permissions' numerical IDs.
	 *
	 * @param permIds
	 * 		the IDs of the permissions
	 */
	public CPermGet(int... permIds) {
		super("permget");
		final ArrayParameter p = new ArrayParameter(permIds.length);
		for (int permId : permIds) {
			p.add(new KeyValueParam("permid", permId));
		}
		add(p);
	}
}
