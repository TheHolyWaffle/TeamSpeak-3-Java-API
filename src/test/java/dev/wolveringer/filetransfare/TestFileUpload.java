package dev.wolveringer.filetransfare;

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

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.junit.Test;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransfare;

public class TestFileUpload {
	
	@Test
	public void test() {
		final TS3Config config = new TS3Config();
		config.setHost(System.getProperty("tsHost"));
		config.setDebugLevel(Level.ALL);

		final TS3Query query = new TS3Query(config);
		query.connect();

		final TS3Api api = query.getApi();
		api.login(System.getProperty("tsUsername"), System.getProperty("tsPassword"));
		api.selectVirtualServerById(Integer.getInteger("tsServerId", 1));
		api.setNickname(System.getProperty("tsNickname", "FileTest [WolverinDEV]"));
		
		byte[] data = getFileBytes(new File("/home/wolverindev/Downloads/teamspeaklogo.png"));
		FileTransfare trans = api.initFileUpload("/icon_100001", data.length);
		api.uploadFile(trans, data);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		query.exit();
	}
	
	private byte[] getFileBytes(File file){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		FileInputStream is = null;
		try{
			is = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int readed = 0;
			while (true) {
				readed = is.read(buffer);
				if(readed == -1)
					break;
				os.write(buffer, 0, readed);
			}
			return os.toByteArray();
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try {
				if(is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if(os != null)
					os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
