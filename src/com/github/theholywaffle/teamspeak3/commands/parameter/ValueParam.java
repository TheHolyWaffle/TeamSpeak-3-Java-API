package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.StringUtil;

public class ValueParam extends Parameter {
	
	private String value;

	public ValueParam(String value){
		this.value=value;
	}

	public String build() {
		return StringUtil.encode(value);
	}

}
