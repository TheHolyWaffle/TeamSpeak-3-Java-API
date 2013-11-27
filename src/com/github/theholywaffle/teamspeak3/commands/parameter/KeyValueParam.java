package com.github.theholywaffle.teamspeak3.commands.parameter;

import com.github.theholywaffle.teamspeak3.StringUtil;

public class KeyValueParam extends Parameter{
	
	private String key;
	private String value;

	public KeyValueParam(String key, String value){
		this.key=key;
		this.value=value;
	}
	
	public KeyValueParam(String key, int value) {
		this(key,value+"");
	}

	public KeyValueParam(String key, long value) {
		this(key,value+"");
	}

	public String build() {
		return StringUtil.encode(key)+"="+StringUtil.encode(value);
	}

}
