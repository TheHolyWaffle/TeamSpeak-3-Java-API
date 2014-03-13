/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.commands.CWhoAmI;

public class KeepAliveThread extends Thread {

	private static final int SLEEP = 60_000;

	private TS3Query ts3;
	private SocketWriter writer;

	private boolean stop;

	public KeepAliveThread(TS3Query ts3, SocketWriter socketWriter) {
		super("Keep alive");
		this.ts3 = ts3;
		this.writer = socketWriter;
	}

	public void run() {
		while (ts3.getSocket() != null && ts3.getSocket().isConnected() && ts3.getIn() != null && !stop) {
			long idleTime = writer.getIdleTime();
			if (idleTime >= SLEEP) {
				ts3.doCommand(new CWhoAmI());
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		TS3Query.log.warning("KeepAlive thread has stopped!");
	}

	public void finish() {
		stop = true;
	}

}
