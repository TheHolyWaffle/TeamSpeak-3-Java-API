package com.github.theholywaffle.teamspeak3.api.wrapper;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 - 2016 Bert De Geyter, Roger Baumgartner
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
public class FileList extends Wrapper {
	
	//name=Pic1.PNG size=563783 datetime=1259425462 type=1
	@ToString
	@AllArgsConstructor
	@Getter
	public static class TS3File {
		private String name;
		private int size;
		private long time;
		private int type;
	}
	
	private List<TS3File> files = new ArrayList<>();
	
	public FileList(List<Wrapper> data) {
		super(new HashMap<String, String>());
		for(Wrapper w : data)
			files.add(new TS3File(w.get("name"), w.getInt("size"), w.getLong("datetime"), w.getInt("type")));
	}
	
	public List<TS3File> getFiles(){
		return Collections.unmodifiableList(files);
	}
	
	public TS3File getFile(String name){
		return getFile(name, true);
	}
	
	public TS3File getFile(String name,boolean ignoreCase){
		for(TS3File file : getFiles())
			if(file.getName().equals(name) || (ignoreCase && file.getName().equalsIgnoreCase(name)))
				return file;
		return null;
	}
	
}
