package com.github.theholywaffle.teamspeak3.commands;

public final class CommandEncoding {

	private CommandEncoding() {}

	public static String encode(String str) {
		str = str.replace("\\", "\\\\");

		str = str.replace(" ", "\\s");
		str = str.replace("/", "\\/");
		str = str.replace("|", "\\p");
		str = str.replace("\b", "\\b");
		str = str.replace("\f", "\\f");
		str = str.replace("\n", "\\n");
		str = str.replace("\r", "\\r");
		str = str.replace("\t", "\\t");
		str = str.replace(String.valueOf((char) 7), "\\a");
		str = str.replace(String.valueOf((char) 11), "\\v");

		return str;
	}

	public static String decode(String str) {
		str = str.replace("\\s", " ");
		str = str.replace("\\/", "/");
		str = str.replace("\\p", "|");
		str = str.replace("\\b", "\b");
		str = str.replace("\\f", "\f");
		str = str.replace("\\n", "\n");
		str = str.replace("\\r", "\r");
		str = str.replace("\\t", "\t");
		str = str.replace("\\a", String.valueOf((char) 7)); // Bell
		str = str.replace("\\v", String.valueOf((char) 11)); // Vertical Tab

		str = str.replace("\\\\", "\\");

		return str;
	}
}
