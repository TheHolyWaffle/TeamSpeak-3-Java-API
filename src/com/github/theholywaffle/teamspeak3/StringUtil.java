package com.github.theholywaffle.teamspeak3;

public class StringUtil {

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

		Character cBell = new Character((char) 7); // \a (not supported by Java)
		Character cVTab = new Character((char) 11); // \v (not supported by
													// Java)

		str = str.replace(cBell.toString(), "\\a");
		str = str.replace(cVTab.toString(), "\\v");

		return str;
	}

	public static String decode(String str) {
		str = str.replace("\\\\", "\\[$mksave]");
		str = str.replace("\\s", " ");
		str = str.replace("\\/", "/");
		str = str.replace("\\p", "|");
		str = str.replace("\\b", "\b");
		str = str.replace("\\f", "\f");
		str = str.replace("\\n", "\n");
		str = str.replace("\\r", "\r");
		str = str.replace("\\t", "\t");

		Character cBell = new Character((char) 7); // \a (not supported by Java)
		Character cVTab = new Character((char) 11); // \v (not supported by
													// Java)

		str = str.replace("\\a", cBell.toString());
		str = str.replace("\\v", cVTab.toString());

		str = str.replace("\\[$mksave]", "\\");
		return str;
	}

	public static boolean getBoolean(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		return StringUtil.getInt(str) == 1;
	}

	public static double getDouble(String str) {
		if (str == null || str.isEmpty()) {
			return -1D;
		}
		return Double.parseDouble(str);
	}

	public static long getLong(String str) {
		if (str == null || str.isEmpty()) {
			return -1L;
		}
		return Long.parseLong(str);
	}

	public static int getInt(String str) {
		if (str == null || str.isEmpty()) {
			return -1;
		}
		return Integer.parseInt(str);
	}

}
