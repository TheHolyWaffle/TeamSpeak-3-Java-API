package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
