/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3;

import java.util.logging.Level;

import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;

public class TS3Config {

	private String host = null;
	private int queryPort = 10011;
	private FloodRate floodRate = FloodRate.DEFAULT;
	private Level level = Level.WARNING;
	private String username = null;
	private String password = null;
	private boolean debugToFile = false;

	public TS3Config setHost(String host) {
		this.host = host;
		return this;
	}

	String getHost() {
		return host;
	}

	public TS3Config setQueryPort(int queryPort) {
		this.queryPort = queryPort;
		return this;
	}

	int getQueryPort() {
		return queryPort;
	}

	public TS3Config setFloodRate(FloodRate rate) {
		this.floodRate = rate;
		return this;
	}

	FloodRate getFloodRate() {
		return floodRate;
	}

	public TS3Config setDebugLevel(Level level) {
		this.level = level;
		return this;
	}

	Level getDebugLevel() {
		return level;
	}

	public TS3Config setLoginCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	String getUsername() {
		return username;
	}

	String getPassword() {
		return password;
	}

	public TS3Config setDebugToFile(boolean debugToFile) {
		this.debugToFile = debugToFile;
		return this;
	}

	boolean getDebugToFile() {
		return debugToFile;
	}

}
