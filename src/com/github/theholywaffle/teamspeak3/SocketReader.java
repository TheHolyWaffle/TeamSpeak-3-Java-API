package com.github.theholywaffle.teamspeak3;

import java.io.IOException;

import com.github.theholywaffle.teamspeak3.commands.Command;

public class SocketReader extends Thread {

	private TS3Query ts3;

	public SocketReader(TS3Query ts3) {
		this.ts3 = ts3;
		try {
			int i = 0;
			while (i < 4) {
				if (ts3.getIn().ready()) {
					TS3Query.log.info("< " + ts3.getIn().readLine());
					i++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (ts3.getSocket().isConnected() && ts3.getIn() != null) {
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
							TS3Query.log.info("[" + c.getName() + "] < " + line);
							if (line.startsWith("error")) {
								c.feedError(line.substring("error ".length()));
								if (c.getError().getId() != 0) {
									TS3Query.log.severe("[ERROR] "+c.getError());
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
		TS3Query.log.severe("SocketReader has a problem!");
	}

}
