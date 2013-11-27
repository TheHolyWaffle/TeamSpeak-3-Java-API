package com.github.theholywaffle.teamspeak3.commands.parameter;

public class RawParam extends Parameter{

	private String raw;

	public RawParam(String raw) {
		this.raw=raw;
	}

	public String build() {
		return raw;
	}

}
