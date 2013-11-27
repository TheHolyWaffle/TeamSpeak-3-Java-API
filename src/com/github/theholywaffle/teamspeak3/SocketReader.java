package com.github.theholywaffle.teamspeak3;

import java.io.IOException;

import com.github.theholywaffle.teamspeak3.api.exception.UnknownErrorException;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.response.QueryError;

public class SocketReader extends Thread {

	private TS3Query ts3;

	private boolean errorFound = false;

	public SocketReader(TS3Query ts3) {
		this.ts3 = ts3;
		try {
			int i = 0;
			while (i < 4) {
				if (ts3.getIn().ready()) {
					TS3Query.log("< " + ts3.getIn().readLine());
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

					Command c = ts3.getCommandList().getFirstNotAnswered();
					if (line.startsWith("notify")) {
						new Thread(new Runnable() {

							public void run() {
								String arr[] = line.split(" ", 2);
								ts3.getEventManager().fireEvent(arr[0], arr[1]);

							}
						}).start();

					} else if (c != null && c.isSent()) {
						TS3Query.log("[" + c.getName() + "] < " + line);
						if (line.startsWith("error")) {
							c.feedError(new QueryError(line.substring("error "
									.length())));
							if (c.getError().getId() != 0) {
								try {
									throw new UnknownErrorException(
											"[ERROR] id:"
													+ c.getError().getId()
													+ " msg:"
													+ c.getError().getMessage());
								} catch (UnknownErrorException e) {
									e.printStackTrace();
								}
							}
							errorFound = true;
						} else if (line.isEmpty() && errorFound) {
							c.setAnswered();
							errorFound = false;
						} else if (!line.isEmpty()) {
							c.feed(line);
						}
					} else {
						TS3Query.log("< " + line);
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
		TS3Query.log("SocketReader has a problem!", true);
	}

}
