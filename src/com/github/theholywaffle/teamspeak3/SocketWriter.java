package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.commands.Command;

public class SocketWriter extends Thread {
	private TS3Query ts3;
	private int floodRate;

	public SocketWriter(TS3Query ts3, int floodRate) {
		this.ts3 = ts3;
		if (floodRate > 50) {
			this.floodRate = floodRate;
		} else {
			this.floodRate = 50;
		}
	}

	public void run() {
		while (ts3.getSocket().isConnected() && ts3.getOut() != null) {
			Command c = ts3.getCommandList().peek();
			if (c != null && !c.isSent()) {
				String msg = c.toString();
				TS3Query.log("> " + msg);
				ts3.getOut().println(msg);
				c.setSent();
			}
		}
		try {
			Thread.sleep(floodRate);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		TS3Query.log("SocketWriter has a problem!", true);
	}
}
