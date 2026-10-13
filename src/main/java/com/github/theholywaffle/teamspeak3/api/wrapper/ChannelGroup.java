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
        return requireInt("cgid");
    }

    public String getName() {
        return require("name");
    }

    /** @return the group type, or {@link PermissionGroupDatabaseType#UNKNOWN} for a type this library does not know */
    public PermissionGroupDatabaseType getType() {
        return requireCode("type", PermissionGroupDatabaseType::fromIndex);
    }

    public long getIconId() {
        return requireLong("iconid");
    }

    public boolean isSavedInDatabase() {
        return requireBoolean("savedb");
    }

    public int getSortId() {
        return requireInt("sortid");
    }

    public int getNameMode() {
        return requireInt("namemode");
    }

    public int getModifyPower() {
        return requireInt("n_modifyp");
    }

    public int getMemberAddPower() {
        return requireInt("n_member_addp");
    }

    public int getMemberRemovePower() {
        return requireInt("n_member_removep");
    }
}
