/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.example;

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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;

public class ChatBotExample {
	
	public static void main(String[] args) {
		new ChatBotExample();
	}

	public ChatBotExample() {
		final TS3Config config = new TS3Config();
		config.setHost("188.40.60.18");
		config.setDebugLevel(Level.ALL);
		config.setLoginCredentials("chronicbot", "phdaHBBD");
		
		final TS3Query query = new TS3Query(config);
		query.connect();
		
		final TS3Api api = query.getApi();
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		api.sendChannelMessage("PutPutBot is online!");
		
		api.registerAllEvents();
		api.addTS3Listeners(new TS3Listener() {
			
			public void onTextMessage(TextMessageEvent e) {
				if(e.getTargetMode()== TextMessageTargetMode.CHANNEL){//Only react to channel messages
					if(e.getMessage().equals("!ping")){
						api.sendChannelMessage("pong");
					}
					if(e.getMessage().toLowerCase().contains("hello")){
						api.sendChannelMessage("Hello "+e.getInvokerName());
					}
					if(e.getMessage().toLowerCase().contains("!test")){
						char a = 'ä';
						char o = 'ö';
						char u = 'ü';
						api.sendChannelMessage("Umlaute: \u00e4 \u00f6 \u00fc \u00c4 \u00d6 \u00dc \u00df");
						api.sendChannelMessage("Umlaute: ä ö ü Ä Ö Ü");
						api.sendChannelMessage("Umlaute: " + a + " " + o + " " + u);
						api.sendChannelMessage(new String("Umlaute: ä ö ü Ä Ö Ü".getBytes(), Charset.forName("UTF8")));
						byte[] m = "Umlaute: ä ö ü Ä Ö Ü".getBytes(Charset.forName("UTF8"));
						api.sendChannelMessage(m.toString());
						//Hallo\s& #228;\s& #246;\s& #252;\s& #223;
						api.sendChannelMessage("Umlaute: &#228; &#246; &#252; &#223");
						api.sendChannelMessage(Charset.forName("UTF8").decode(ByteBuffer.wrap(m)).toString());
						api.sendChannelMessage("Ã¤Ã¶Ã¼Ã„Ã–Ãœ");
					}
				}
			}
			
			public void onServerEdit(ServerEditedEvent e) {
								
			}
			
			public void onClientMoved(ClientMovedEvent e) {
								
			}
			
			public void onClientLeave(ClientLeaveEvent e) {
								
			}
			
			public void onClientJoin(ClientJoinEvent e) {
								
			}
			
			public void onChannelEdit(ChannelEditedEvent e) {
								
			}
			
			public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
								
			}

			@Override
			public void onChannelCreate(ChannelCreateEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChannelDeleted(ChannelDeletedEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChannelMoved(ChannelMovedEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	

}
