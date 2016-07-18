package dev.wolveringer.filetransfare;

import static org.junit.Assert.*;

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