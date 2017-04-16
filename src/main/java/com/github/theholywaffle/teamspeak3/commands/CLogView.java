package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

import java.util.ArrayList;
import java.util.List;

public class CLogView extends Command {
	public CLogView(int lines, int offset) {
		super("logview");

		if(lines > 0) { add(new KeyValueParam("lines", lines)); }
		if(offset > 0) { add(new KeyValueParam("begin_pos", offset)); }
	}

	public List<String> getLines() {
		List<Wrapper> responses = getResponse();
		List<String> lines = new ArrayList<>(responses.size());

		for(Wrapper r : responses) {
			lines.add(r.getMap().get("l"));
		}
		return lines;
	}
}