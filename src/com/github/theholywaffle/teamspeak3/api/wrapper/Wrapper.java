package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

import com.github.theholywaffle.teamspeak3.StringUtil;
import com.github.theholywaffle.teamspeak3.api.Property;

public class Wrapper {

	private HashMap<String, String> map;

	public Wrapper(HashMap<String, String> map) {
		this.map = map;
	}

	public HashMap<String, String> getMap() {
		return map;
	}

	protected boolean getBoolean(String str) {
		if (str.isEmpty()) {
			return false;
		}
		return StringUtil.getBoolean(get(str));
	}

	protected boolean getBoolean(Property p) {
		return getBoolean(p.getName());
	}

	protected double getDouble(String str) {
		if (str.isEmpty()) {
			return -1D;
		}
		return StringUtil.getDouble(get(str));
	}

	protected double getDouble(Property p) {
		return getDouble(p.getName());
	}

	protected long getLong(String str) {
		if (str.isEmpty()) {
			return -1L;
		}
		return StringUtil.getLong(get(str));
	}

	protected long getLong(Property p) {
		return getLong(p.getName());
	}

	protected int getInt(String str) {
		if (str.isEmpty()) {
			return -1;
		}
		return StringUtil.getInt(get(str));
	}

	protected int getInt(Property p) {
		return getInt(p.getName());
	}

	/**
	 * Only use this if you know what you are doing.
	 * 
	 */
	public String get(String str) {
		String result = map.get(str);
		return result != null ? result : "";
	}

	/**
	 * Only use this if you know what you are doing.
	 * 
	 */
	public String get(Property p) {
		return get(p.getName());
	}
	
	public String toString(){
		return map.toString();
	}

}
