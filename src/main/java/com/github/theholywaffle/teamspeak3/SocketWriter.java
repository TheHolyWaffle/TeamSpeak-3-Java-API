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

import com.github.theholywaffle.teamspeak3.commands.Command;

public class SocketWriter extends Thread {
	private TS3Query ts3;
	private int floodRate;
	private long lastCommand = System.currentTimeMillis();
	private boolean stop;

	public SocketWriter(TS3Query ts3, int floodRate) {
		super("SocketWriter");
		this.ts3 = ts3;
		if (floodRate > 50) {
			this.floodRate = floodRate;
		} else {
			this.floodRate = 50;
		}
	}

	public void run() {
		while (ts3.getSocket()!= null &&ts3.getSocket().isConnected() && ts3.getOut() != null && !stop) {
			Command c = ts3.getCommandList().peek();
			if (c != null && !c.isSent()) {
				String msg = c.toString();
				TS3Query.log.info("> " + msg);
				ts3.getOut().println(msg);
				lastCommand = System.currentTimeMillis();
				c.setSent();
			}
			try {
				Thread.sleep(floodRate);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	

		TS3Query.log.warning("SocketWriter has stopped!");
	}

	public long getIdleTime() {
		return System.currentTimeMillis() - lastCommand;
	}

	public void finish() {
		stop = true;
	}
}
