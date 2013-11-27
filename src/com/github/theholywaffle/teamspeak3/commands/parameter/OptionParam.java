package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.StringUtil;

public class OptionParam extends Parameter {

	private String option;

	public OptionParam(String option) {
		this.option=option;
	}

	public String build() {
		return "-"+StringUtil.encode(option);
	}

}
