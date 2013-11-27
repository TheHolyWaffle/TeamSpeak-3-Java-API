package com.github.theholywaffle.teamspeak3.api;

public class Snapshot{

	private String snapshot;

	public Snapshot(String snapshot) {
		this.snapshot=snapshot;
	}
	
	public String get(){
		return snapshot;
	}

}
