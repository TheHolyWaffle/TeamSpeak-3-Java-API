package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2015 Bert De Geyter, Roger Baumgartner
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

public final class CommandEncoding {

	private CommandEncoding() {}

	public static String encode(String str) {
		StringBuilder encoded = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
				case '\\' -> encoded.append("\\\\");
				case ' ' -> encoded.append("\\s");
				case '/' -> encoded.append("\\/");
				case '|' -> encoded.append("\\p");
				case '\b' -> encoded.append("\\b");
				case '\f' -> encoded.append("\\f");
				case '\n' -> encoded.append("\\n");
				case '\r' -> encoded.append("\\r");
				case '\t' -> encoded.append("\\t");
				case 7 -> encoded.append("\\a");
				case 11 -> encoded.append("\\v");
				default -> encoded.append(ch);
			}
		}
		return encoded.toString();
	}

	/**
	 * Decodes each escape once. Unknown escapes and a trailing backslash are
	 * preserved verbatim, allowing forward-compatible and malformed values.
	 *
	 * @param str the encoded value
	 * @return the decoded value
	 */
	public static String decode(String str) {
		StringBuilder decoded = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch != '\\' || i + 1 == str.length()) {
				decoded.append(ch);
				continue;
			}
			char escaped = str.charAt(++i);
			switch (escaped) {
				case '\\' -> decoded.append('\\');
				case 's' -> decoded.append(' ');
				case '/' -> decoded.append('/');
				case 'p' -> decoded.append('|');
				case 'b' -> decoded.append('\b');
				case 'f' -> decoded.append('\f');
				case 'n' -> decoded.append('\n');
				case 'r' -> decoded.append('\r');
				case 't' -> decoded.append('\t');
				case 'a' -> decoded.append((char) 7);
				case 'v' -> decoded.append((char) 11);
				default -> decoded.append('\\').append(escaped);
			}
		}
		return decoded.toString();
	}
}
