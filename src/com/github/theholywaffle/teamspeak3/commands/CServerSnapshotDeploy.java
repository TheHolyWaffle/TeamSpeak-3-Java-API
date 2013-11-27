package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;

public class CServerSnapshotDeploy extends Command {

	public CServerSnapshotDeploy(String snapshot) {
		super("serversnapshotdeploy");
		add(new RawParam(snapshot));
	}

}
