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

import java.util.Locale;

public enum ServerInstanceProperty implements Property {

	CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL(false),
	CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL(false),
	CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL(false),
	CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL(false),
	CONNECTION_BYTES_RECEIVED_TOTAL(false),
	CONNECTION_BYTES_SENT_TOTAL(false),
	CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED(false),
	CONNECTION_FILETRANSFER_BANDWIDTH_SENT(false),
	CONNECTION_FILETRANSFER_BYTES_RECEIVED_TOTAL(false),
	CONNECTION_FILETRANSFER_BYTES_SENT_TOTAL(false),
	CONNECTION_PACKETS_RECEIVED_TOTAL(false),
	CONNECTION_PACKETS_SENT_TOTAL(false),
	HOST_TIMESTAMP_UTC(false),
	INSTANCE_UPTIME(false),
	SERVERINSTANCE_DATABASE_VERSION(false),
	SERVERINSTANCE_FILETRANSFER_PORT(true),
	SERVERINSTANCE_GUEST_SERVERQUERY_GROUP(true),
	SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH(true),
	SERVERINSTANCE_MAX_UPLOAD_TOTAL_BANDWIDTH(true),
	SERVERINSTANCE_PERMISSIONS_VERSION(false),
	SERVERINSTANCE_SERVERQUERY_BAN_TIME(true),
	SERVERINSTANCE_SERVERQUERY_FLOOD_COMMANDS(true),
	SERVERINSTANCE_SERVERQUERY_FLOOD_TIME(true),
	SERVERINSTANCE_TEMPLATE_CHANNELADMIN_GROUP(true),
	SERVERINSTANCE_TEMPLATE_CHANNELDEFAULT_GROUP(true),
	SERVERINSTANCE_TEMPLATE_SERVERADMIN_GROUP(true),
	SERVERINSTANCE_TEMPLATE_SERVERDEFAULT_GROUP(true),
	VIRTUALSERVERS_RUNNING_TOTAL(false),
	VIRTUALSERVERS_TOTAL_CHANNELS_ONLINE(false),
	VIRTUALSERVERS_TOTAL_CLIENTS_ONLINE(false),
	VIRTUALSERVERS_TOTAL_MAXCLIENTS(false);

	private final boolean changeable;

	ServerInstanceProperty(boolean changeable) {
		this.changeable = changeable;
	}

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean isChangeable() {
		return changeable;
	}

}
