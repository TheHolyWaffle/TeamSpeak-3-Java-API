package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.commands.CWhoAmI;

public class KeepAliveThread extends Thread{
	
	private static final int SLEEP = 60_000;

	private TS3Query ts3;

	public KeepAliveThread(TS3Query ts3) {
		this.ts3=ts3;
	}
	
	public void run(){
		while (ts3.getSocket().isConnected() && ts3.getIn() != null) {
			ts3.doCommand(new CWhoAmI());
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		TS3Query.log("KeepAlive thread is having problems!", true);
	}

}
