package com.github.theholywaffle.teamspeak3;

import java.util.concurrent.CopyOnWriteArrayList;

import com.github.theholywaffle.teamspeak3.commands.Command;

public class CommandList extends CopyOnWriteArrayList<Command> {

	private static final long serialVersionUID = -831895630613713668L;

	public Command getFirstNotSent() {
		for (Command c : this) {
			if (!c.isSent()) {
				return c;
			}
		}
		return null;
	}

	public Command getFirstNotAnswered() {
		for (Command c : this) {
			if (!c.isAnswered()) {
				return c;
			}
		}
		return null;
	}

	public String toString() {
		String str = "";
		for (int i = 0; i < size(); i++) {
			Command c = get(i);
			str += "[" + c.getName() + "]";
			str += c.isSent() ? "Sent" : "Not Sent";
			str+=",";
			str += c.isAnswered() ? "Answered" : "Not Answered";
			str+="<>";
		}
		return str;
	}

}
