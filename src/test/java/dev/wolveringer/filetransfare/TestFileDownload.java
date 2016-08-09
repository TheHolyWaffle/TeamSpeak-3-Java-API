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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.junit.Test;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransfare;

public class TestFileDownload {

	@Test
	public void test() throws IOException{
		final TS3Config config = new TS3Config();
		config.setHost(System.getProperty("tsHost"));
		config.setDebugLevel(Level.ALL);

		final TS3Query query = new TS3Query(config);
		query.connect();

		final TS3Api api = query.getApi();
		api.login(System.getProperty("tsUsername"), System.getProperty("tsPassword"));
		api.selectVirtualServerById(Integer.getInteger("tsServerId", 1));
		api.setNickname(System.getProperty("tsNickname", "FileTest [WolverinDEV]"));
		
		FileTransfare trans = api.initFileDownload("/icon_100001");
		byte[] data = api.downloadFile(trans);
		new ImageDisplay(data).display();
		try {
			Thread.sleep(50000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		query.exit();
	}
}

class ImageDisplay extends JFrame{
	private BufferedImage image;
	
	public ImageDisplay(byte[] data) throws IOException {
		this.image = ImageIO.read(new ByteArrayInputStream(data));
	}
	
	@Override
	public void paint(Graphics g) {
		g.drawImage(image, (int) (getSize().getWidth()/2)-(image.getWidth()/2), (int) (getSize().getHeight()/2)-(image.getHeight()/2), this);
	}
	
	public void display(){
		setSize(Math.max(image.getWidth(), 100), Math.max(image.getHeight(), 100));
		setVisible(true);
	}
}