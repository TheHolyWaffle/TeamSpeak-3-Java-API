/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.PermissionGroupDatabaseType;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.api.ServerGroupType;
import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.wrapper.AdvancedPermission;
import com.github.theholywaffle.teamspeak3.api.wrapper.Ban;
import com.github.theholywaffle.teamspeak3.api.wrapper.Binding;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroupClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Complaint;
import com.github.theholywaffle.teamspeak3.api.wrapper.ConnectionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.HostInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.InstanceInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Message;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.PermissionInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.PrivilegeKey;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroupClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Version;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServer;
import com.github.theholywaffle.teamspeak3.api.wrapper.VirtualServerInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.*;

public class TS3Api {

	private final TS3Query query;

	public TS3Api(TS3Query query) {
		this.query = query;
	}

	/**
	 * 
	 * Adds a new ban entry.
	 * 
	 * @param ip
	 *            target ip-adress, can be null
	 * @param name
	 *            target name, can be null.
	 * @param uid
	 *            target UID, can be null
	 * @param timeInSeconds
	 *            the duration of the ban, 0 equals permanent
	 * @param reason
	 *            the reason for the ban
	 * @return banid
	 */
	public int addBan(String ip, String name, String uid, long timeInSeconds,
			String reason) {
		CBanAdd add = new CBanAdd(ip, name, uid, timeInSeconds, reason);
		if (query.doCommand(add)) {
			return StringUtil.getInt(add.getFirstResponse().get("banid"));
		}
		return -1;

	}

	public boolean addChannelClientPermission(int channelId, int clientDBId,
			String permName, int permValue) {
		CChannelClientAddPerm add = new CChannelClientAddPerm(channelId,
				clientDBId, permName, permValue);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public int addChannelGroup(String name) {
		return addChannelGroup(name, null);
	}

	public int addChannelGroup(String name, PermissionGroupDatabaseType t) {
		CChannelGroupAdd add = new CChannelGroupAdd(name, t);
		if (query.doCommand(add)) {
			return StringUtil.getInt(add.getFirstResponse().get("cgid"));
		}
		return -1;
	}

	public boolean addChannelPermission(int channelId, String permName,
			int permValue) {
		CChannelAddPerm perm = new CChannelAddPerm(channelId, permName,
				permValue);
		if (query.doCommand(perm)) {
			return perm.getError().isSuccessful();
		}
		return false;
	}

	public boolean addClientPermission(int clientDBId, String permName,
			int permValue, boolean permSkipped) {
		CClientAddPerm add = new CClientAddPerm(clientDBId, permName,
				permValue, permSkipped);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public boolean addClientToServerGroup(int groupId, int clientDatabaseId) {
		CServerGroupAddClient add = new CServerGroupAddClient(groupId,
				clientDatabaseId);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public boolean addComplain(int clientDBId, String text) {
		CComplainAdd add = new CComplainAdd(clientDBId, text);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public boolean addPermissionToAllServerGroups(ServerGroupType t,
			String permName, int permValue, boolean permNegated,
			boolean permSkipped) {
		CServerGroupAutoAddPerm add = new CServerGroupAutoAddPerm(t, permName,
				permValue, permNegated, permSkipped);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public boolean addPermissionToChannelGroup(int groupId, String permName,
			int permValue) {
		CChannelGroupAddPerm add = new CChannelGroupAddPerm(groupId, permName,
				permValue);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public String addPrivilegeKey(int type, int groupId, int channelId,
			String description) {
		CPrivilegeKeyAdd add = new CPrivilegeKeyAdd(type, groupId, channelId,
				description);
		if (query.doCommand(add)) {
			return add.getFirstResponse().get("token");
		}
		return null;
	}

	public String addPrivilegeKeyChannelGroup(int channelGroupId,
			int channelId, String description) {
		return addPrivilegeKey(1, channelGroupId, channelId, description);
	}

	public String addPrivilegeKeyServerGroup(int serverGroupId,
			String description) {
		return addPrivilegeKey(0, serverGroupId, 0, description);
	}

	public int addServerGroup(String name) {
		return addServerGroup(name, PermissionGroupDatabaseType.REGULAR);
	}

	public int addServerGroup(String name, PermissionGroupDatabaseType t) {
		CServerGroupAdd add = new CServerGroupAdd(name, t);
		if (query.doCommand(add)) {
			return StringUtil.getInt(add.getFirstResponse().get("sgid"));
		}
		return -1;
	}

	public boolean addServerGroupPermission(int groupId, String permName,
			int value, boolean negated, boolean skipped) {
		CServerGroupAddPerm add = new CServerGroupAddPerm(groupId, permName,
				value, negated, skipped);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public void addTS3Listeners(TS3Listener... l) {
		query.getEventManager().addListeners(l);
	}

	public int banClient(int clientId, long timeInSeconds) {
		return banClient(clientId, timeInSeconds, null);
	}

	public int banClient(int clientId, long timeInSeconds, String reason) {
		CBanClient client = new CBanClient(clientId, timeInSeconds, reason);
		if (query.doCommand(client)) {
			return StringUtil.getInt(client.getFirstResponse().get("banid"));
		}
		return -1;
	}

	public int banClient(int clientId, String reason) {
		return banClient(clientId, reason);
	}

	public boolean broadcast(String message) {
		CGM broadcast = new CGM(message);
		if (query.doCommand(broadcast)) {
			return broadcast.getError().isSuccessful();
		}
		return false;
	}

	public boolean copyChannelGroup(int sourceGroupId, int targetGroupId,
			PermissionGroupDatabaseType t) {
		CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId,
				targetGroupId, t);
		if (query.doCommand(copy)) {
			return copy.getError().isSuccessful();
		}
		return false;
	}

	public int copyChannelGroup(int sourceGroupId, String targetName,
			PermissionGroupDatabaseType t) {
		CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId,
				targetName, t);
		if (query.doCommand(copy)) {
			return StringUtil.getInt(copy.getFirstResponse().get("cgid"));
		}
		return -1;
	}

	public int copyServerGroup(int idSource, int idTarget,
			PermissionGroupDatabaseType t) {
		return copyServerGroup(idSource, idTarget, "ignored", t);
	}

	private int copyServerGroup(int idSource, int idTarget, String name,
			PermissionGroupDatabaseType t) {
		CServerGroupCopy copy = new CServerGroupCopy(idSource, idTarget, name,
				t);
		if (query.doCommand(copy)) {
			return StringUtil.getInt(copy.getFirstResponse().get("sgid"));
		}
		return -1;
	}

	public int copyServerGroup(int idSource, String name,
			PermissionGroupDatabaseType t) {
		return copyServerGroup(idSource, 0, name, t);
	}

	public int createChannel(String name,
			HashMap<ChannelProperty, String> options) {
		CChannelCreate create = new CChannelCreate(name, options);
		if (query.doCommand(create)) {
			return StringUtil.getInt(create.getFirstResponse().get("cid"));
		}
		return -1;
	}

	public boolean createServer(String name,
			HashMap<VirtualServerProperty, String> map) {
		CServerCreate create = new CServerCreate(name, map);
		if (query.doCommand(create)) {
			return create.getError().isSuccessful();
		}
		return false;
	}

	public String createServerQueryLogin(String name) {
		CClientSetServerQueryLogin login = new CClientSetServerQueryLogin(name);
		if (query.doCommand(login)) {
			return login.getFirstResponse().get("client_login_password");
		}
		return null;
	}

	public Snapshot createServerSnapshot() {
		CServerSnapshotCreate create = new CServerSnapshotCreate();
		if (query.doCommand(create)) {
			return new Snapshot(create.getRaw());
		}
		return null;
	}

	public boolean deleteAllBans() {
		CBanDelAll del = new CBanDelAll();
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteAllComplaints(int clientDBId) {
		CComplainDelAll del = new CComplainDelAll(clientDBId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteBan(int banId) {
		CBanDel del = new CBanDel(banId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteChannel(int channelId) {
		CChannelDelete del = new CChannelDelete(channelId, true);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteChannelClientPermission(int channelId, int clientDBId,
			String permName) {
		CChannelClientDelPerm del = new CChannelClientDelPerm(channelId,
				clientDBId, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteChannelGroup(int groupId) {
		CChannelGroupDel del = new CChannelGroupDel(groupId, true);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteChannelGroupPermission(int groupId, String permName) {
		CChannelGroupDelPerm del = new CChannelGroupDelPerm(groupId, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteChannelPermission(int channelId, String permName) {
		CChannelDelPerm del = new CChannelDelPerm(channelId, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteClientPermission(int clientDBId, String permName) {
		CClientDelPerm del = new CClientDelPerm(clientDBId, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteComplaint(int targetClientDBId, int fromClientDBId) {
		CComplainDel del = new CComplainDel(targetClientDBId, fromClientDBId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteDatabaseClientProperties(int clientDBId) {
		CClientDBDelelete del = new CClientDBDelelete(clientDBId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteOfflineMessage(int messageId) {
		CMessageDel del = new CMessageDel(messageId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deletePermissionFromAllServerGroups(ServerGroupType t,
			String permName) {
		CServerGroupAutoDelPerm del = new CServerGroupAutoDelPerm(t, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deletePrivilegeKey(String token) {
		CPrivilegeKeyDelete del = new CPrivilegeKeyDelete(token);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteServer(int id) {
		CServerDelete delete = new CServerDelete(id);
		if (query.doCommand(delete)) {
			return delete.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteServerGroup(int id) {
		return deleteServerGroup(id, true);
	}

	public boolean deleteServerGroup(int id, boolean forced) {
		CServerGroupDel del = new CServerGroupDel(id, forced);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deleteServerGroupPermission(int groupId, String permName) {
		CServerGroupDelPerm del = new CServerGroupDelPerm(groupId, permName);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public boolean deployServerSnapshot(Snapshot snapshot) {
		CServerSnapshotDeploy deploy = new CServerSnapshotDeploy(snapshot.get());
		if (query.doCommand(deploy)) {
			return deploy.getError().isSuccessful();
		}
		return false;
	}

	public boolean editChannel(int channelId,
			HashMap<ChannelProperty, String> options) {
		CChannelEdit edit = new CChannelEdit(channelId, options);
		if (query.doCommand(edit)) {
			return edit.getError().isSuccessful();
		}
		return false;
	}

	/**
	 * So far only ClientProperty.CLIENT_DESCRIPTION seems to a correct
	 * ClientProperty. Others don't.
	 */
	public void editClient(int clientId, HashMap<ClientProperty, String> options) {
		CClientEdit edit = new CClientEdit(clientId, options);
		query.doCommand(edit);
	}

	public boolean editDatabaseClient(int clientDBId,
			HashMap<ClientProperty, String> options) {
		CClientDBEdit edit = new CClientDBEdit(clientDBId, options);
		if (query.doCommand(edit)) {
			return edit.getError().isSuccessful();
		}
		return false;
	}

	public boolean editInstance(ServerInstanceProperty p, String value) {
		if (p.isChangeable()) {
			CInstanceEdit edit = new CInstanceEdit(p, value);
			if (query.doCommand(edit)) {
				return edit.getError().isSuccessful();
			}
		}
		return false;
	}

	public boolean editServer(HashMap<VirtualServerProperty, String> map) {
		CServerEdit edit = new CServerEdit(map);
		if (query.doCommand(edit)) {
			return edit.getError().isSuccessful();
		}
		return false;
	}

	public List<Ban> getBans() {
		CBanList list = new CBanList();
		if (query.doCommand(list)) {
			List<Ban> bans = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				bans.add(new Ban(opt));
			}
			return bans;
		}
		return null;
	}

	public List<Binding> getBindings() {
		CBindingList list = new CBindingList();
		if (query.doCommand(list)) {
			List<Binding> bindings = new ArrayList<>();
			for (HashMap<String, String> map : list.getResponse()) {
				bindings.add(new Binding(map));
			}
			return bindings;
		}
		return null;
	}

	public Channel getChannelByName(String name) {
		CChannelFind find = new CChannelFind(name);
		if (query.doCommand(find)) {
			for (Channel c : getChannels()) {
				if (c.getId() == StringUtil.getInt(find.getFirstResponse().get(
						"cid"))) {
					return c;
				}
			}
		}
		return null;
	}

	public List<Permission> getChannelClientPermissions(int channelId,
			int clientDBId) {
		CChannelClientPermList list = new CChannelClientPermList(channelId,
				clientDBId);
		if (query.doCommand(list)) {
			List<Permission> permissions = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				permissions.add(new Permission(opt));
			}
			return permissions;
		}
		return null;
	}

	/**
	 * Use -1 to ingore an argument.
	 * 
	 */
	public List<ChannelGroupClient> getChannelGroupClients(int channelId,
			int clientDBId, int groupId) {
		CChannelGroupClientList list = new CChannelGroupClientList(channelId,
				clientDBId, groupId);
		if (query.doCommand(list)) {
			List<ChannelGroupClient> clients = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				clients.add(new ChannelGroupClient(opt));
			}
			return clients;
		}
		return null;
	}

	public List<ChannelGroupClient> getChannelGroupClientsByChannelGroupId(
			int groupId) {
		return getChannelGroupClients(-1, -1, groupId);
	}

	public List<ChannelGroupClient> getChannelGroupClientsByChannelId(
			int channelId) {
		return getChannelGroupClients(channelId, -1, -1);
	}

	public List<ChannelGroupClient> getChannelGroupClientsByClientDBId(
			int clientDBId) {
		return getChannelGroupClients(-1, clientDBId, -1);
	}

	public List<Permission> getChannelGroupPermissions(int groupId) {
		CChannelGroupPermList list = new CChannelGroupPermList(groupId);
		if (query.doCommand(list)) {
			List<Permission> p = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				p.add(new Permission(opt));
			}
			return p;
		}
		return null;
	}

	public List<ChannelGroup> getChannelGroups() {
		CChannelGroupList list = new CChannelGroupList();
		if (query.doCommand(list)) {
			List<ChannelGroup> groups = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				groups.add(new ChannelGroup(opt));
			}
			return groups;
		}
		return null;
	}

	public ChannelInfo getChannelInfo(int channelId) {
		CChannelInfo info = new CChannelInfo(channelId);
		if (query.doCommand(info)) {
			return new ChannelInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public List<Permission> getChannelPermissions(int channelId) {
		CChannelPermList list = new CChannelPermList(channelId);
		if (query.doCommand(list)) {
			List<Permission> p = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				p.add(new Permission(opt));
			}
			return p;
		}
		return null;
	}

	public List<Channel> getChannels() {
		CChannelList list = new CChannelList();
		if (query.doCommand(list)) {
			List<Channel> channels = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				channels.add(new Channel(opt));
			}
			return channels;
		}
		return null;
	}

	public List<Client> getClientByName(String pattern) {
		CClientFind find = new CClientFind(pattern);
		if (query.doCommand(find)) {
			List<Client> clients = new ArrayList<>();
			for (Client c : getClients()) {
				for (HashMap<String, String> opt : find.getResponse()) {
					if (c.getId() == StringUtil.getInt(new Wrapper(opt)
							.get("clid"))) {
						clients.add(c);
					}
				}
			}
			return clients;
		}
		return null;
	}

	public ClientInfo getClientByUId(String clientUId) {
		CClientGetIds get = new CClientGetIds(clientUId);
		if (query.doCommand(get)) {
			return getClientInfo(StringUtil.getInt(get.getFirstResponse()
					.getMap().get("clid")));
		}
		return null;
	}

	public ClientInfo getClientInfo(int clientId) {
		CClientInfo info = new CClientInfo(clientId);
		if (query.doCommand(info)) {
			return new ClientInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public List<Permission> getClientPermissions(int clientDBId) {
		CClientPermList list = new CClientPermList(clientDBId);
		if (query.doCommand(list)) {
			List<Permission> permissions = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				permissions.add(new Permission(opt));
			}
			return permissions;
		}
		return null;
	}

	public List<Client> getClients() {
		CClientList list = new CClientList();
		if (query.doCommand(list)) {
			List<Client> clients = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				clients.add(new Client(opt));
			}
			return clients;
		}
		return null;
	}

	public List<Complaint> getComplaints() {
		return getComplaints(-1);
	}

	public List<Complaint> getComplaints(int clientDBId) {
		CComplainList list = new CComplainList(clientDBId);
		if (query.doCommand(list)) {
			List<Complaint> complaints = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				complaints.add(new Complaint(opt));
			}
			return complaints;
		}
		return null;
	}

	public ConnectionInfo getConnectionInfo() {
		CServerRequestConnectionInfo info = new CServerRequestConnectionInfo();
		if (query.doCommand(info)) {
			return new ConnectionInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public DatabaseClientInfo getDatabaseClientByName(String name) {
		CClientDBFind find = new CClientDBFind(name, false);
		if (query.doCommand(find)) {
			return getDatabaseClientInfo(StringUtil.getInt(find
					.getFirstResponse().get("cldbid")));
		}
		return null;
	}

	public DatabaseClientInfo getDatabaseClientByUId(String clientUId) {
		CClientGetDBIdFromUId get = new CClientGetDBIdFromUId(clientUId);
		if (query.doCommand(get)) {
			return getDatabaseClientInfo(StringUtil.getInt(get
					.getFirstResponse().get("cldbid")));
		}
		return null;
	}

	public DatabaseClientInfo getDatabaseClientInfo(int clientDBId) {
		CClientDBInfo info = new CClientDBInfo(clientDBId);
		if (query.doCommand(info)) {
			return new DatabaseClientInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	/**
	 * Be warned, this method takes quite some time to execute.
	 */
	public List<DatabaseClient> getDatabaseClients() {
		CClientDBList countList = new CClientDBList(0, 1, true);
		if (query.doCommand(countList)) {
			int count = StringUtil.getInt(countList.getFirstResponse().get(
					"count"));
			int i = 0;
			List<DatabaseClient> clients = new ArrayList<>();
			while (i < count) {
				CClientDBList list = new CClientDBList(i, 200, false);
				if (query.doCommand(list)) {
					for (HashMap<String, String> map : list.getResponse()) {
						clients.add(new DatabaseClient(map));
					}
				}
				i += 200;
			}
			return clients;
		}
		return null;
	}

	public HostInfo getHostInfo() {
		CHostInfo info = new CHostInfo();
		if (query.doCommand(info)) {
			return new HostInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public InstanceInfo getInstanceInfo() {
		CInstanceInfo info = new CInstanceInfo();
		if (query.doCommand(info)) {
			return new InstanceInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public String getOfflineMessage(int messageId) {
		CMessageGet get = new CMessageGet(messageId);
		if (query.doCommand(get)) {
			return get.getFirstResponse().get("message");
		}
		return null;
	}

	public List<Message> getOfflineMessages() {
		CMessageList list = new CMessageList();
		if (query.doCommand(list)) {
			List<Message> msg = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				msg.add(new Message(opt));
			}
			return msg;
		}
		return null;
	}

	public void getPermission(String permName) {
		CPermFind find = new CPermFind(permName);
		if (query.doCommand(find)) {

		}
	}

	public int getPermissionIdByName(String permName) {
		CPermIdGetByName get = new CPermIdGetByName(permName);
		if (query.doCommand(get)) {
			return StringUtil.getInt(get.getFirstResponse().get("permid"));
		}
		return -1;
	}

	public List<AdvancedPermission> getPermissionOverview(int channelId,
			int clientDBId) {
		CPermOverview overview = new CPermOverview(channelId, clientDBId);
		if (query.doCommand(overview)) {
			List<AdvancedPermission> permissions = new ArrayList<>();
			for (HashMap<String, String> opt : overview.getResponse()) {
				permissions.add(new AdvancedPermission(opt));
			}
			return permissions;
		}
		return null;
	}

	public List<PermissionInfo> getPermissions() {
		CPermissionList list = new CPermissionList();
		if (query.doCommand(list)) {
			List<PermissionInfo> permissions = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				permissions.add(new PermissionInfo(opt));
			}
			return permissions;
		}
		return null;
	}

	public int getPermissionValue(String permName) {
		CPermGet get = new CPermGet(permName);
		if (query.doCommand(get)) {
			return StringUtil.getInt(get.getFirstResponse().get("permvalue"));
		}
		return -1;
	}

	public List<PrivilegeKey> getPrivilegeKeys() {
		CPrivilegeKeyList list = new CPrivilegeKeyList();
		if (query.doCommand(list)) {
			List<PrivilegeKey> keys = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				keys.add(new PrivilegeKey(opt));
			}
			return keys;
		}
		return null;
	}

	public List<ServerGroupClient> getServerGroupClients(int groupId) {
		CServerGroupClientList list = new CServerGroupClientList(groupId);
		if (query.doCommand(list)) {
			List<ServerGroupClient> clients = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				clients.add(new ServerGroupClient(opt));
			}
			return clients;
		}
		return null;
	}

	public List<Permission> getServerGroupPermissions(int id) {
		CServerGroupPermList list = new CServerGroupPermList(id);
		if (query.doCommand(list)) {
			List<Permission> p = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				p.add(new Permission(opt));
			}
			return p;
		}
		return null;
	}

	public List<ServerGroup> getServerGroups() {
		CServerGroupList list = new CServerGroupList();
		if (query.doCommand(list)) {
			List<ServerGroup> groups = new ArrayList<>();
			for (HashMap<String, String> opt : list.getResponse()) {
				groups.add(new ServerGroup(opt));
			}
			return groups;
		}
		return null;
	}

	public List<ServerGroup> getServerGroupsByClientId(int clientDatabaseId) {
		CServerGroupsByClientId client = new CServerGroupsByClientId(
				clientDatabaseId);
		if (query.doCommand(client)) {
			List<ServerGroup> list = new ArrayList<>();
			List<ServerGroup> allGroups = getServerGroups();
			for (HashMap<String, String> opt : client.getResponse()) {
				for (ServerGroup s : allGroups) {
					if (s.getId() == StringUtil.getInt(opt.get("sgid"))) {
						list.add(s);
					}
				}
			}
			return list;
		}
		return null;
	}

	public int getServerIdByPort(int port) {
		CServerIdGetByPort s = new CServerIdGetByPort(port);
		if (query.doCommand(s)) {
			return StringUtil.getInt(s.getFirstResponse().get("server_id"));
		}
		return -1;
	}

	public VirtualServerInfo getServerInfo() {
		CServerInfo info = new CServerInfo();
		if (query.doCommand(info)) {
			return new VirtualServerInfo(info.getFirstResponse().getMap());
		}
		return null;
	}

	public Version getVersion() {
		CVersion version = new CVersion();
		if (query.doCommand(version)) {
			return new Version(version.getFirstResponse().getMap());
		}
		return null;
	}

	public List<VirtualServer> getVirtualServers() {
		CServerList serverList = new CServerList();
		if (query.doCommand(serverList)) {
			ArrayList<VirtualServer> servers = new ArrayList<>();
			for (HashMap<String, String> opt : serverList.getResponse()) {
				servers.add((new VirtualServer(opt)));
			}
			return servers;
		}
		return null;
	}

	public boolean kickClientFromChannel(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, null,
				clientIds);
	}

	public boolean kickClientFromChannel(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, message,
				clientIds);
	}

	public boolean kickClientFromServer(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, null, clientIds);
	}

	public boolean kickClientFromServer(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, message,
				clientIds);
	}

	private boolean kickClients(ReasonIdentifier reason, String message,
			int... clientIds) {
		CClientKick kick = new CClientKick(reason, message, clientIds);
		if (query.doCommand(kick)) {
			return kick.getError().isSuccessful();
		}
		return false;
	}

	public boolean login(String username, String password) {
		CLogin login = new CLogin(username, password);
		if (query.doCommand(login)) {
			return login.getError().isSuccessful();
		}
		return false;
	}

	public boolean logout() {
		CLogout logout = new CLogout();
		if (query.doCommand(logout)) {
			return true;
		}
		return false;
	}

	public void moveChannel(int channelId, int channelTargetId) {
		moveChannel(channelId, channelTargetId, -1);
	}

	public boolean moveChannel(int channelId, int channelTargetId, int order) {
		CChannelMove move = new CChannelMove(channelId, channelTargetId, order);
		if (query.doCommand(move)) {
			return move.getError().isSuccessful();
		}
		return false;
	}

	public boolean moveClient(int clientId, int channelId) {
		return moveClient(clientId, channelId, null);
	}

	public boolean moveClient(int clientId, int channelId,
			String channelPassword) {
		CClientMove move = new CClientMove(clientId, channelId, channelPassword);
		if (query.doCommand(move)) {
			return move.getError().isSuccessful();
		}
		return false;
	}

	public boolean moveClient(int channelId) {
		return moveClient(whoAmI().getId(), channelId);
	}
	
	public boolean moveClient(int channelId, String channelPassword) {
		return moveClient(whoAmI().getId(), channelId,channelPassword);
	}

	public boolean pokeClient(int clientId, String message) {
		CClientPoke poke = new CClientPoke(clientId, message);
		if (query.doCommand(poke)) {
			return poke.getError().isSuccessful();
		}
		return false;
	}

	public boolean quit() {
		CQuit quit = new CQuit();
		if (query.doCommand(quit)) {
			System.exit(0);
			return true;
		}
		return false;
	}

	public void registerAllEvents() {
		registerEvent(TS3EventType.CHANNEL, whoAmI().getChannelId());
		registerEvent(TS3EventType.SERVER);
		registerEvent(TS3EventType.TEXT_CHANNEL);
		registerEvent(TS3EventType.TEXT_PRIVATE);
		registerEvent(TS3EventType.TEXT_SERVER);
	}

	public boolean registerEvent(TS3EventType t) {
		return registerEvent(t, -1);
	}

	public boolean registerEvent(TS3EventType t, int channelId) {
		CServerNotifyRegister r = new CServerNotifyRegister(t, channelId);
		if (query.doCommand(r)) {
			return r.getError().isSuccessful();
		}
		return false;
	}

	public void registerEvents(TS3EventType... t) {
		for (TS3EventType type : t) {
			registerEvent(type, -1);
		}
	}

	public boolean removeClientFromServerGroup(int groupId, int clientDatabaseId) {
		CServerGroupDelClient del = new CServerGroupDelClient(groupId,
				clientDatabaseId);
		if (query.doCommand(del)) {
			return del.getError().isSuccessful();
		}
		return false;
	}

	public void removeTS3Listeners(TS3Listener... l) {
		query.getEventManager().removeListeners(l);
	}

	public boolean renameChannelGroup(int groupId, String name) {
		CChannelGroupRename rename = new CChannelGroupRename(groupId, name);
		if (query.doCommand(rename)) {
			return rename.getError().isSuccessful();
		}
		return false;
	}

	public boolean renameServerGroup(int id, String name) {
		CServerGroupRename rename = new CServerGroupRename(id, name);
		if (query.doCommand(rename)) {
			return rename.getError().isSuccessful();
		}
		return false;
	}

	/**
	 * Resets all permissions and deletes all server/channel groups. Use
	 * carefully.
	 * 
	 * @return A new administrator account
	 */
	public String resetPermissions() {
		CPermReset reset = new CPermReset();
		if (query.doCommand(reset)) {
			return reset.getFirstResponse().get("token");
		}
		return null;
	}

	public boolean selectVirtualServerById(int id) {
		CUse use = new CUse(id, -1);
		if (query.doCommand(use)) {
			return use.getError().isSuccessful();
		}
		return false;
	}

	public boolean selectVirtualServerByPort(int port) {
		CUse use = new CUse(-1, port);
		if (query.doCommand(use)) {
			return use.getError().isSuccessful();
		}
		return false;
	}

	public boolean selectVirtualServer(VirtualServer server) {
		return selectVirtualServerById(server.getId());
	}

	public boolean sendOfflineMessage(String clientUId, String subject,
			String message) {
		CMessageAdd add = new CMessageAdd(clientUId, subject, message);
		if (query.doCommand(add)) {
			return add.getError().isSuccessful();
		}
		return false;
	}

	public boolean sendTextMessage(TextMessageTargetMode targetMode,
			int targetId, String message) {
		CSendTextMessage msg = new CSendTextMessage(targetMode.getIndex(),
				targetId, message);
		if (query.doCommand(msg)) {
			return msg.getError().isSuccessful();
		}
		return false;
	}

	public boolean sendChannelMessage(int channelId, String message) {
		return sendTextMessage(TextMessageTargetMode.CHANNEL, channelId,
				message);
	}

	public boolean sendChannelMessage(String message) {
		return sendChannelMessage(whoAmI().getChannelId(), message);
	}

	public boolean sendServerMessage(int serverId, String message) {
		return sendTextMessage(TextMessageTargetMode.SERVER, serverId, message);
	}

	public boolean sendServerMessage(String message) {
		return sendServerMessage(1, message);
	}

	public boolean sendPrivateMessage(int clientId, String message) {
		return sendTextMessage(TextMessageTargetMode.CLIENT, clientId, message);
	}

	public boolean setClientChannelGroup(int groupId, int channelId,
			int clientDBId) {
		CSetClientChannelGroup group = new CSetClientChannelGroup(groupId,
				channelId, clientDBId);
		if (query.doCommand(group)) {
			return group.getError().isSuccessful();
		}
		return false;
	}

	public boolean setMessageRead(int messageId) {
		return setMessageReadFlag(messageId, true);
	}

	public boolean setMessageReadFlag(int messageId, boolean read) {
		CMessageUpdateFlag flag = new CMessageUpdateFlag(messageId, read);
		if (query.doCommand(flag)) {
			return flag.getError().isSuccessful();
		}
		return read;
	}

	public boolean setNickname(String name) {
		HashMap<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, name);
		return updateClient(options);
	}

	public boolean startServer(int id) {
		CServerStart start = new CServerStart(id);
		if (query.doCommand(start)) {
			return start.getError().isSuccessful();
		}
		return false;
	}

	public boolean stopServer(int id) {
		CServerStop start = new CServerStop(id);
		if (query.doCommand(start)) {
			return start.getError().isSuccessful();
		}
		return false;
	}

	public boolean stopServerProcess() {
		CServerProcessStop stop = new CServerProcessStop();
		if (query.doCommand(stop)) {
			return stop.getError().isSuccessful();
		}
		return false;
	}

	public boolean unregisterAllEvents() {
		CServerNotifyUnregister unr = new CServerNotifyUnregister();
		if (query.doCommand(unr)) {
			return unr.getError().isSuccessful();
		}
		return false;
	}

	public boolean updateClient(HashMap<ClientProperty, String> options) {
		CClientUpdate update = new CClientUpdate(options);
		if (query.doCommand(update)) {
			return update.getError().isSuccessful();
		}
		return false;
	}

	public boolean usePrivilegeKey(String token) {
		CPrivilegeKeyUse use = new CPrivilegeKeyUse(token);
		if (query.doCommand(use)) {
			return use.getError().isSuccessful();
		}
		return false;
	}

	public ServerQueryInfo whoAmI() {
		CWhoAmI whoAmI = new CWhoAmI();
		if (query.doCommand(whoAmI)) {
			return new ServerQueryInfo(whoAmI.getFirstResponse().getMap());
		}
		return null;
	}

}
