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

/**
 * Describes a single permission on the TS3 server.
 * <p>
 * Includes the numerical ID and the name of the permission, as well
 * as an optional short description of this permission. Does not
 * include any information about any possible permission assignments.
 * </p><p>
 * For a complete description of the TS3 permission system, refer to
 * <a href="http://forum.teamspeak.com/threads/49581-The-New-Permission-Documentataions">
 * this post</a> on the TeamSpeak forums.
 * </p>
 */
public class PermissionInfo extends Wrapper {

	public PermissionInfo(Map<String, String> map) {
		super(map);
	}

	/**
	 * Gets the numerical ID of this permission.
	 * <p>
	 * In most cases, the name of the permission should be
	 * preferred over the numerical ID.
	 * </p>
	 *
	 * @return this permission's numerical ID
	 */
	public int getId() {
		return getInt("permid");
	}

	/**
	 * Gets the name of this permission.
	 * <p>
	 * Boolean permissions are prefixed with {@code b_}<br>
	 * Integer permissions are prefixed with {@code i_}
	 * </p>
	 *
	 * @return this permission's name
	 */
	public String getName() {
		return get("permname");
	}

	/**
	 * A short description about what this permission does.
	 * <p>
	 * Does not exist for all permissions. In that case, an
	 * empty String will be returned instead.
	 * </p>
	 *
	 * @return a short description of this permission
	 */
	public String getDescription() {
		return get("permdesc");
	}

}
