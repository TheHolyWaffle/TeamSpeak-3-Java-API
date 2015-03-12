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

import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;

import java.util.Map;

public class ChannelGroup extends Wrapper {

	public ChannelGroup(Map<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt("cgid");
	}

	/**
	 * @deprecated Use {@link #getId()} instead.
	 */
	@Deprecated
	public int getGroupId() {
		return getId();
	}

	public String getName() {
		return get("name");
	}

	public PermissionGroupDatabaseType getType() {
		final int type = getInt("type");
		for (final PermissionGroupDatabaseType t : PermissionGroupDatabaseType.values()) {
			if (t.getIndex() == type) {
				return t;
			}
		}
		return null;
	}

	public long getIconId() {
		return getLong("iconid");
	}

	public boolean isSavedInDatabase() {
		return getBoolean("savedb");
	}

	public int getSortId() {
		return getInt("sortid");
	}

	public int getNameMode() {
		return getInt("namemode");
	}

	public int getModifyPower() {
		return getInt("n_modifyp");
	}

	public int getMemberAddPower() {
		return getInt("n_member_addp");
	}

	public int getMemberRemovePower() {
		return getInt("n_member_removep");
	}

}
