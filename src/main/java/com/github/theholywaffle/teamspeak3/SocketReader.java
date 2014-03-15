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

import java.io.IOException;

import com.github.theholywaffle.teamspeak3.commands.Command;

public class SocketReader extends Thread {

	private TS3Query ts3;
	private volatile boolean stop;

	public SocketReader(TS3Query ts3) {
		super("SocketReader");
		this.ts3 = ts3;
		try {
			int i = 0;
			while (i < 4 || ts3.getIn().ready()) {
				TS3Query.log.info("< " + ts3.getIn().readLine());
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (ts3.getSocket()!= null && ts3.getSocket().isConnected() && ts3.getIn() != null && !stop) {
			try {
				if (ts3.getIn().ready()) {
					final String line = ts3.getIn().readLine();
					if (!line.isEmpty()) {
						Command c = ts3.getCommandList().peek();
						if (line.startsWith("notify")) {
							TS3Query.log.info("< [event] " + line);
							new Thread(new Runnable() {

								public void run() {
									String arr[] = line.split(" ", 2);
									ts3.getEventManager().fireEvent(arr[0],
											arr[1]);

								}
							}).start();

						} else if (c != null && c.isSent()) {
							TS3Query.log
									.info("[" + c.getName() + "] < " + line);
							if (line.startsWith("error")) {
								c.feedError(line.substring("error ".length()));
								if (c.getError().getId() != 0) {
									TS3Query.log.severe("[ERROR] "
											+ c.getError());
								}
								c.setAnswered();
								ts3.getCommandList().remove(c);
							} else if (!line.isEmpty()) {
								c.feed(line);
							}
						} else {
							TS3Query.log.info("< " + line);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		TS3Query.log.warning("SocketReader has stopped!");
	}

	public void finish() {
		stop = true;
	}

}
