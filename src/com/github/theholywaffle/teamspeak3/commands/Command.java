package com.github.theholywaffle.teamspeak3.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

public class Command {

	private String command;
	private boolean sent = false;
	private boolean answered = false;
	private DefaultArrayResponse response;
	private QueryError error;
	private ArrayList<Parameter> params = new ArrayList<>();
	private String raw;

	public Command(String command) {
		this.command = command;
	}

	protected void add(Parameter p) {
		params.add(p);
	}

	public void feed(String str) {
		raw = str;
		if (response == null) {
			response = new DefaultArrayResponse(str);
		}
	}

	public void feedError(String err) {
		if(error == null){
			error = new QueryError(new DefaultArrayResponse(err).getArray().get(0));
		}
	}

	public QueryError getError() {
		return error;
	}

	public String getName() {
		return command;
	}

	public Wrapper getFirstResponse() {
		List<HashMap<String, String>> resp = getResponse();
		if(resp.size() > 0){
			return new Wrapper(resp.get(0));
		}
		return new Wrapper(new HashMap<String, String>());
	}

	public List<HashMap<String, String>> getResponse() {
		if (response == null) {
			return new DefaultArrayResponse("").getArray();
		}
		return response.getArray();
	}

	public boolean isAnswered() {
		return answered;
	}

	public boolean isSent() {
		return sent;
	}

	public void setAnswered() {
		answered = true;
	}

	public void setSent() {
		sent = true;
	}

	public String toString() {
		String str = command;
		for (Parameter p : params) {
			str += " " + p;
		}
		return str;
	}

	public String getRaw() {
		return raw;
	}

}
