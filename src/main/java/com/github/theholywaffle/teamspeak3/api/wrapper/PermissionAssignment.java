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

import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;

import java.util.Map;

/**
 * Describes a single permission that is assigned to a varying target.
 * <p>
 * This class is used when a lot of permissions are sent at once.
 * To reduce bandwidth usage, the TS3 server only transmit the numeric
 * permission ID and not the permission name.
 * </p><p>
 * For a complete description of the TS3 permission system, refer to
 * <a href="http://forum.teamspeak.com/threads/49581-The-New-Permission-Documentataions">
 * this post</a> on the TeamSpeak forums.
 * </p>
 */
public class PermissionAssignment extends Wrapper {

	public PermissionAssignment(Map<String, String> map) {
		super(map);
	}

	/**
	 * Where this permission assignment originates from.
	 *
	 * @return the type of this permission assignment
	 */
	public PermissionGroupType getType() {
		final int type = getInt("t");
		for (final PermissionGroupType p : PermissionGroupType.values()) {
			if (p.getIndex() == type) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Specifies where this permission assignment originates from,
	 * depending on the {@linkplain #getType() type} of this assignment.
	 * <p>
	 * {@code x -> y} := <i>In case {@code type} is {@code x}, {@code majorId} means {@code y}</i>
	 * </p><p>
	 * {@linkplain PermissionGroupType#SERVER_GROUP SERVER_GROUP} {@code ->} {@linkplain ServerGroup#getId() Server group ID}<br>
	 * {@linkplain PermissionGroupType#GLOBAL_CLIENT GLOBAL_CLIENT} {@code ->} {@linkplain Client#getDatabaseId() Client database ID}<br>
	 * {@linkplain PermissionGroupType#CHANNEL CHANNEL} {@code ->} {@linkplain Channel#getId() Channel ID}<br>
	 * {@linkplain PermissionGroupType#CHANNEL_GROUP CHANNEL_GROUP} {@code ->} {@linkplain Channel#getId() Channel ID}<br>
	 * {@linkplain PermissionGroupType#CHANNEL_CLIENT CHANNEL_CLIENT} {@code ->} {@linkplain Channel#getId() Channel ID}
	 * </p>
	 *
	 * @return the major ID of the source of this assignment as described above
	 */
	public int getMajorId() {
		return getInt("id1");
	}

	/**
	 * Specifies where this permission assignment originates from,
	 * depending on the {@linkplain #getType() type} of this assignment.
	 * <p>
	 * {@code x -> y} := <i>In case {@code type} is {@code x}, {@code minorId} means {@code y}</i>
	 * </p><p>
	 * {@linkplain PermissionGroupType#CHANNEL_GROUP CHANNEL_GROUP} {@code ->} {@linkplain ChannelGroup#getId() Channel group ID}<br>
	 * {@linkplain PermissionGroupType#CHANNEL_CLIENT CHANNEL_CLIENT} {@code ->} {@linkplain Client#getDatabaseId() Client database ID}
	 * </p><p>
	 * Otherwise {@code getMinorId()} is undefined should return {@code 0}.
	 * </p>
	 *
	 * @return the minor ID of the source of this assignment as described above
	 */
	public int getMinorId() {
		return getInt("id2");
	}

	/**
	 * Gets the numerical ID of this permission.
	 *
	 * @return this permission's numerical ID
	 */
	public int getId() {
		return getInt("p");
	}

	/**
	 * Gets the value of this permission assignment.
	 * <p>
	 * Please note that this value doesn't necessarily have to be
	 * the effective permission value for a client, as this assignment
	 * can be overridden by another assignment.
	 * </p><p>
	 * Integer permissions usually have values between 0 and 100,
	 * but any integer value is theoretically valid.
	 * </p><p>
	 * Boolean permissions have a value of {@code 0} to represent
	 * {@code false} and {@code 1} to represent {@code true}.
	 * </p>
	 *
	 * @return the value of this permission assignment
	 */
	public int getValue() {
		return getInt("v");
	}

	/**
	 * Returns {@code true} if this permission is negated.
	 * <p>
	 * Negated means that instead of the highest value, the lowest
	 * value will be selected for this permission instead.
	 * </p>
	 *
	 * @return whether this permission is negated or not
	 */
	public boolean isNegated() {
		return getBoolean("n");
	}

	/**
	 * Returns {@code true} if this permission is skipped.
	 * <p>
	 * Skipped only exists for server group and client permissions,
	 * therefore this value will always be false for channel group permissions.
	 * </p><p>
	 * If a client permission is skipped, it won't be overridden by channel
	 * group permissions.<br>
	 * If a server group permission is skipped, it won't be overridden by
	 * channel group or client permissions.
	 * </p>
	 *
	 * @return whether this permission is negated or not
	 */
	public boolean isSkipped() {
		return getBoolean("s");
	}
}
