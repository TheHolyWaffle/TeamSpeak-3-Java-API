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

import com.github.theholywaffle.teamspeak3.api.*;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.*;
import com.github.theholywaffle.teamspeak3.commands.*;

import java.util.*;

public class TS3ApiAsync {

	private final TS3Query query;

	public TS3ApiAsync(TS3Query query) {
		this.query = query;
	}

	/**
	 * Adds a new ban entry.
	 *
	 * @param ip
	 * 		target ip-adress, can be null
	 * @param name
	 * 		target name, can be null.
	 * @param uid
	 * 		target UID, can be null
	 * @param timeInSeconds
	 * 		the duration of the ban, 0 equals permanent
	 * @param reason
	 * 		the reason for the ban
	 *
	 * @return banid
	 */
	public CommandFuture<Integer> addBan(String ip, String name, String uid, long timeInSeconds, String reason) {
		final CBanAdd add = new CBanAdd(ip, name, uid, timeInSeconds, reason);
		return executeAndReturnIntProperty(add, "banid");
	}

	public CommandFuture<Boolean> addChannelClientPermission(int channelId, int clientDBId, String permName, int permValue) {
		final CChannelClientAddPerm add = new CChannelClientAddPerm(channelId, clientDBId, permName, permValue);
		return executeAndReturnError(add);
	}

	public CommandFuture<Integer> addChannelGroup(String name) {
		return addChannelGroup(name, null);
	}

	public CommandFuture<Integer> addChannelGroup(String name, PermissionGroupDatabaseType t) {
		final CChannelGroupAdd add = new CChannelGroupAdd(name, t);
		return executeAndReturnIntProperty(add, "cgid");
	}

	public CommandFuture<Boolean> addChannelPermission(int channelId, String permName, int permValue) {
		final CChannelAddPerm perm = new CChannelAddPerm(channelId, permName, permValue);
		return executeAndReturnError(perm);
	}

	public CommandFuture<Boolean> addClientPermission(int clientDBId, String permName, int permValue, boolean permSkipped) {
		final CClientAddPerm add = new CClientAddPerm(clientDBId, permName, permValue, permSkipped);
		return executeAndReturnError(add);
	}

	public CommandFuture<Boolean> addClientToServerGroup(int groupId, int clientDatabaseId) {
		final CServerGroupAddClient add = new CServerGroupAddClient(groupId, clientDatabaseId);
		return executeAndReturnError(add);
	}

	public CommandFuture<Boolean> addComplain(int clientDBId, String text) {
		final CComplainAdd add = new CComplainAdd(clientDBId, text);
		return executeAndReturnError(add);
	}

	public CommandFuture<Boolean> addPermissionToAllServerGroups(ServerGroupType t, String permName, int permValue, boolean permNegated,
																 boolean permSkipped) {
		final CServerGroupAutoAddPerm add = new CServerGroupAutoAddPerm(t, permName, permValue, permNegated, permSkipped);
		return executeAndReturnError(add);
	}

	public CommandFuture<Boolean> addPermissionToChannelGroup(int groupId, String permName, int permValue) {
		final CChannelGroupAddPerm add = new CChannelGroupAddPerm(groupId, permName, permValue);
		return executeAndReturnError(add);
	}

	public CommandFuture<String> addPrivilegeKey(int type, int groupId, int channelId, String description) {
		final CPrivilegeKeyAdd add = new CPrivilegeKeyAdd(type, groupId, channelId, description);
		return executeAndReturnStringProperty(add, "token");
	}

	public CommandFuture<String> addPrivilegeKeyChannelGroup(int channelGroupId, int channelId, String description) {
		return addPrivilegeKey(1, channelGroupId, channelId, description);
	}

	public CommandFuture<String> addPrivilegeKeyServerGroup(int serverGroupId, String description) {
		return addPrivilegeKey(0, serverGroupId, 0, description);
	}

	public CommandFuture<Integer> addServerGroup(String name) {
		return addServerGroup(name, PermissionGroupDatabaseType.REGULAR);
	}

	public CommandFuture<Integer> addServerGroup(String name, PermissionGroupDatabaseType t) {
		final CServerGroupAdd add = new CServerGroupAdd(name, t);
		return executeAndReturnIntProperty(add, "sgid");
	}

	public CommandFuture<Boolean> addServerGroupPermission(int groupId, String permName, int value, boolean negated, boolean skipped) {
		final CServerGroupAddPerm add = new CServerGroupAddPerm(groupId, permName, value, negated, skipped);
		return executeAndReturnError(add);
	}

	public void addTS3Listeners(TS3Listener... l) {
		query.getEventManager().addListeners(l);
	}

	public CommandFuture<Integer> banClient(int clientId, long timeInSeconds) {
		return banClient(clientId, timeInSeconds, null);
	}

	public CommandFuture<Integer> banClient(int clientId, long timeInSeconds, String reason) {
		final CBanClient client = new CBanClient(clientId, timeInSeconds, reason);
		return executeAndReturnIntProperty(client, "banid");
	}

	public CommandFuture<Integer> banClient(int clientId, String reason) {
		return banClient(clientId, 0, reason);
	}

	public CommandFuture<Boolean> broadcast(String message) {
		final CGM broadcast = new CGM(message);
		return executeAndReturnError(broadcast);
	}

	public CommandFuture<Boolean> copyChannelGroup(int sourceGroupId, int targetGroupId, PermissionGroupDatabaseType t) {
		final CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId, targetGroupId, t);
		return executeAndReturnError(copy);
	}

	public CommandFuture<Integer> copyChannelGroup(int sourceGroupId, String targetName, PermissionGroupDatabaseType t) {
		final CChannelGroupCopy copy = new CChannelGroupCopy(sourceGroupId, targetName, t);
		return executeAndReturnIntProperty(copy, "cgid");
	}

	public CommandFuture<Integer> copyServerGroup(int idSource, int idTarget, PermissionGroupDatabaseType t) {
		return copyServerGroup(idSource, idTarget, "ignored", t);
	}

	private CommandFuture<Integer> copyServerGroup(int idSource, int idTarget, String name, PermissionGroupDatabaseType t) {
		final CServerGroupCopy copy = new CServerGroupCopy(idSource, idTarget, name, t);
		return executeAndReturnIntProperty(copy, "sgid");
	}

	public CommandFuture<Integer> copyServerGroup(int idSource, String name, PermissionGroupDatabaseType t) {
		return copyServerGroup(idSource, 0, name, t);
	}

	public CommandFuture<Integer> createChannel(String name, HashMap<ChannelProperty, String> options) {
		final CChannelCreate create = new CChannelCreate(name, options);
		return executeAndReturnIntProperty(create, "cid");
	}

	public CommandFuture<Boolean> createServer(String name, HashMap<VirtualServerProperty, String> map) {
		final CServerCreate create = new CServerCreate(name, map);
		return executeAndReturnError(create);
	}

	public CommandFuture<String> createServerQueryLogin(String name) {
		final CClientSetServerQueryLogin login = new CClientSetServerQueryLogin(name);
		return executeAndReturnStringProperty(login, "client_login_password");
	}

	public CommandFuture<Snapshot> createServerSnapshot() {
		final CServerSnapshotCreate create = new CServerSnapshotCreate();
		final CommandFuture<Snapshot> future = new CommandFuture<>();

		query.doCommandAsync(create, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(create, future)) return;
				future.set(new Snapshot(create.getRaw()));
			}
		});
		return future;
	}

	public CommandFuture<Boolean> deleteAllBans() {
		final CBanDelAll del = new CBanDelAll();
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteAllComplaints(int clientDBId) {
		final CComplainDelAll del = new CComplainDelAll(clientDBId);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteBan(int banId) {
		final CBanDel del = new CBanDel(banId);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteChannel(int channelId) {
		final CChannelDelete del = new CChannelDelete(channelId, true);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteChannelClientPermission(int channelId, int clientDBId, String permName) {
		final CChannelClientDelPerm del = new CChannelClientDelPerm(channelId, clientDBId, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteChannelGroup(int groupId) {
		final CChannelGroupDel del = new CChannelGroupDel(groupId, true);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteChannelGroupPermission(int groupId, String permName) {
		final CChannelGroupDelPerm del = new CChannelGroupDelPerm(groupId, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteChannelPermission(int channelId, String permName) {
		final CChannelDelPerm del = new CChannelDelPerm(channelId, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteClientPermission(int clientDBId, String permName) {
		final CClientDelPerm del = new CClientDelPerm(clientDBId, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteComplaint(int targetClientDBId, int fromClientDBId) {
		final CComplainDel del = new CComplainDel(targetClientDBId, fromClientDBId);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteDatabaseClientProperties(int clientDBId) {
		final CClientDBDelete del = new CClientDBDelete(clientDBId);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteOfflineMessage(int messageId) {
		final CMessageDel del = new CMessageDel(messageId);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deletePermissionFromAllServerGroups(ServerGroupType t, String permName) {
		final CServerGroupAutoDelPerm del = new CServerGroupAutoDelPerm(t, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deletePrivilegeKey(String token) {
		final CPrivilegeKeyDelete del = new CPrivilegeKeyDelete(token);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteServer(int id) {
		final CServerDelete delete = new CServerDelete(id);
		return executeAndReturnError(delete);
	}

	public CommandFuture<Boolean> deleteServerGroup(int id) {
		return deleteServerGroup(id, true);
	}

	public CommandFuture<Boolean> deleteServerGroup(int id, boolean forced) {
		final CServerGroupDel del = new CServerGroupDel(id, forced);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deleteServerGroupPermission(int groupId, String permName) {
		final CServerGroupDelPerm del = new CServerGroupDelPerm(groupId, permName);
		return executeAndReturnError(del);
	}

	public CommandFuture<Boolean> deployServerSnapshot(Snapshot snapshot) {
		final CServerSnapshotDeploy deploy = new CServerSnapshotDeploy(snapshot.get());
		return executeAndReturnError(deploy);
	}

	public CommandFuture<Boolean> editChannel(int channelId, HashMap<ChannelProperty, String> options) {
		final CChannelEdit edit = new CChannelEdit(channelId, options);
		return executeAndReturnError(edit);
	}

	/**
	 * So far only ClientProperty.CLIENT_DESCRIPTION seems to be a correct ClientProperty. Others don't.
	 */
	public CommandFuture<Boolean> editClient(int clientId, HashMap<ClientProperty, String> options) {
		final CClientEdit edit = new CClientEdit(clientId, options);
		return executeAndReturnError(edit);
	}

	public CommandFuture<Boolean> editDatabaseClient(int clientDBId, HashMap<ClientProperty, String> options) {
		final CClientDBEdit edit = new CClientDBEdit(clientDBId, options);
		return executeAndReturnError(edit);
	}

	public CommandFuture<Boolean> editInstance(ServerInstanceProperty p, String value) {
		if (p.isChangeable()) {
			final CInstanceEdit edit = new CInstanceEdit(p, value);
			return executeAndReturnError(edit);
		}
		CommandFuture<Boolean> immediatelyFalse = new CommandFuture<>();
		immediatelyFalse.set(false);
		return immediatelyFalse;
	}

	public CommandFuture<Boolean> editServer(HashMap<VirtualServerProperty, String> map) {
		final CServerEdit edit = new CServerEdit(map);
		return executeAndReturnError(edit);
	}

	public CommandFuture<List<Ban>> getBans() {
		final CBanList list = new CBanList();
		final CommandFuture<List<Ban>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Ban> bans = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					bans.add(new Ban(opt));
				}
				future.set(bans);
			}
		});
		return future;
	}

	public CommandFuture<List<Binding>> getBindings() {
		final CBindingList list = new CBindingList();
		final CommandFuture<List<Binding>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Binding> bindings = new ArrayList<>();
				for (final HashMap<String, String> map : list.getResponse()) {
					bindings.add(new Binding(map));
				}
				future.set(bindings);
			}
		});
		return future;
	}

	public CommandFuture<Channel> getChannelByName(String name) {
		final CChannelFind find = new CChannelFind(name);
		final CommandFuture<Channel> future = new CommandFuture<>();

		getChannels().onSuccess(new CommandFuture.SuccessListener<List<Channel>>() {
			@Override
			public void handleSuccess(final List<Channel> result) {
				query.doCommandAsync(find, new Callback() {
					@Override
					public void handle() {
						if (hasFailed(find, future)) return;

						for (final Channel c : result) {
							if (c.getId() == StringUtil.getInt(find.getFirstResponse().get("cid"))) {
								future.set(c);
								return;
							}
							future.set(null); // Not found
						}
					}
				});
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<List<Permission>> getChannelClientPermissions(int channelId, int clientDBId) {
		final CChannelClientPermList list = new CChannelClientPermList(channelId, clientDBId);
		final CommandFuture<List<Permission>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Permission> permissions = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					permissions.add(new Permission(opt));
				}
				future.set(permissions);
			}
		});
		return future;
	}

	/**
	 * Use -1 to ingore an argument.
	 */
	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClients(int channelId, int clientDBId, int groupId) {
		final CChannelGroupClientList list = new CChannelGroupClientList(channelId, clientDBId, groupId);
		final CommandFuture<List<ChannelGroupClient>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<ChannelGroupClient> clients = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					clients.add(new ChannelGroupClient(opt));
				}
				future.set(clients);
			}
		});
		return future;
	}

	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByChannelGroupId(int groupId) {
		return getChannelGroupClients(-1, -1, groupId);
	}

	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByChannelId(int channelId) {
		return getChannelGroupClients(channelId, -1, -1);
	}

	public CommandFuture<List<ChannelGroupClient>> getChannelGroupClientsByClientDBId(int clientDBId) {
		return getChannelGroupClients(-1, clientDBId, -1);
	}

	public CommandFuture<List<Permission>> getChannelGroupPermissions(int groupId) {
		final CChannelGroupPermList list = new CChannelGroupPermList(groupId);
		final CommandFuture<List<Permission>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Permission> p = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					p.add(new Permission(opt));
				}
				future.set(p);
			}
		});
		return future;
	}

	public CommandFuture<List<ChannelGroup>> getChannelGroups() {
		final CChannelGroupList list = new CChannelGroupList();
		final CommandFuture<List<ChannelGroup>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<ChannelGroup> groups = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					groups.add(new ChannelGroup(opt));
				}
				future.set(groups);
			}
		});
		return future;
	}

	public CommandFuture<ChannelInfo> getChannelInfo(int channelId) {
		final CChannelInfo info = new CChannelInfo(channelId);
		final CommandFuture<ChannelInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new ChannelInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<List<Permission>> getChannelPermissions(int channelId) {
		final CChannelPermList list = new CChannelPermList(channelId);
		final CommandFuture<List<Permission>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Permission> p = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					p.add(new Permission(opt));
				}
				future.set(p);
			}
		});
		return future;
	}

	public CommandFuture<List<Channel>> getChannels() {
		final CChannelList list = new CChannelList();
		final CommandFuture<List<Channel>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Channel> channels = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					channels.add(new Channel(opt));
				}
				future.set(channels);
			}
		});
		return future;
	}

	public CommandFuture<List<Client>> getClientByName(String pattern) {
		final CClientFind find = new CClientFind(pattern);
		final CommandFuture<List<Client>> future = new CommandFuture<>();

		getClients().onSuccess(new CommandFuture.SuccessListener<List<Client>>() {
			@Override
			public void handleSuccess(final List<Client> result) {
				query.doCommandAsync(find, new Callback() {
					@Override
					public void handle() {
						if (hasFailed(find, future)) return;

						final List<Client> clients = new ArrayList<>();
						for (final Client c : result) {
							for (final HashMap<String, String> opt : find.getResponse()) {
								if (c.getId() == StringUtil.getInt(new Wrapper(opt).get("clid"))) {
									clients.add(c);
								}
							}
						}
						future.set(clients);
					}
				});
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<ClientInfo> getClientByUId(String clientUId) {
		final CClientGetIds get = new CClientGetIds(clientUId);
		final CommandFuture<ClientInfo> future = new CommandFuture<>();

		query.doCommandAsync(get, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(get, future)) return;

				getClientInfo(StringUtil.getInt(get.getFirstResponse().getMap().get("clid"))).forwardResult(future);
			}
		});
		return future;
	}

	public CommandFuture<ClientInfo> getClientInfo(int clientId) {
		final CClientInfo info = new CClientInfo(clientId);
		final CommandFuture<ClientInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new ClientInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<List<Permission>> getClientPermissions(int clientDBId) {
		final CClientPermList list = new CClientPermList(clientDBId);
		final CommandFuture<List<Permission>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Permission> permissions = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					permissions.add(new Permission(opt));
				}
				future.set(permissions);
			}
		});
		return future;
	}

	public CommandFuture<List<Client>> getClients() {
		final CClientList list = new CClientList();
		final CommandFuture<List<Client>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Client> clients = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					clients.add(new Client(opt));
				}
				future.set(clients);
			}
		});
		return future;
	}

	public CommandFuture<List<Complaint>> getComplaints() {
		return getComplaints(-1);
	}

	public CommandFuture<List<Complaint>> getComplaints(int clientDBId) {
		final CComplainList list = new CComplainList(clientDBId);
		final CommandFuture<List<Complaint>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Complaint> complaints = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					complaints.add(new Complaint(opt));
				}
				future.set(complaints);
			}
		});
		return future;
	}

	public CommandFuture<ConnectionInfo> getConnectionInfo() {
		final CServerRequestConnectionInfo info = new CServerRequestConnectionInfo();
		final CommandFuture<ConnectionInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new ConnectionInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<DatabaseClientInfo> getDatabaseClientByName(String name) {
		final CClientDBFind find = new CClientDBFind(name, false);
		final CommandFuture<DatabaseClientInfo> future = new CommandFuture<>();

		query.doCommandAsync(find, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(find, future)) return;

				getDatabaseClientInfo(StringUtil.getInt(find.getFirstResponse().get("cldbid"))).forwardResult(future);
			}
		});
		return future;
	}

	public CommandFuture<DatabaseClientInfo> getDatabaseClientByUId(String clientUId) {
		final CClientGetDBIdFromUId get = new CClientGetDBIdFromUId(clientUId);
		final CommandFuture<DatabaseClientInfo> future = new CommandFuture<>();

		query.doCommandAsync(get, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(get, future)) return;

				getDatabaseClientInfo(StringUtil.getInt(get.getFirstResponse().get("cldbid"))).forwardResult(future);
			}
		});
		return future;
	}

	public CommandFuture<DatabaseClientInfo> getDatabaseClientInfo(int clientDBId) {
		final CClientDBInfo info = new CClientDBInfo(clientDBId);
		final CommandFuture<DatabaseClientInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new DatabaseClientInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	/**
	 * Be warned, this method takes quite some time to execute.
	 */
	public CommandFuture<List<DatabaseClient>> getDatabaseClients() {
		final CClientDBList countList = new CClientDBList(0, 1, true);
		final CommandFuture<List<DatabaseClient>> future = new CommandFuture<>();

		query.doCommandAsync(countList, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(countList, future)) return;

				final int count = StringUtil.getInt(countList.getFirstResponse().get("count"));
				final int futuresCount = ((count - 1) / 200) + 1;
				final ArrayList<CommandFuture<List<DatabaseClient>>> futures = new ArrayList<>(futuresCount);
				for (int i = 0; i < count; i += 200) {
					futures.add(getDatabaseClients(i, 200));
				}

				CommandFuture.awaitAll(futures).onSuccess(new CommandFuture.SuccessListener<Collection<List<DatabaseClient>>>() {
					@Override
					public void handleSuccess(Collection<List<DatabaseClient>> result) {
						int total = 0;
						for (List<DatabaseClient> list : result) {
							total += list.size();
						}

						final ArrayList<DatabaseClient> combination = new ArrayList<>(total);
						for (List<DatabaseClient> list : result) {
							combination.addAll(list);
						}
						future.set(combination);
					}
				}).forwardFailure(future);
			}
		});
		return future;
	}

	public CommandFuture<List<DatabaseClient>> getDatabaseClients(final int offset, final int count) {
		final CClientDBList list = new CClientDBList(offset, count, false);
		final CommandFuture<List<DatabaseClient>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<DatabaseClient> clients = new ArrayList<>(count);
				for (final HashMap<String, String> map : list.getResponse()) {
					clients.add(new DatabaseClient(map));
				}
				future.set(clients);
			}
		});
		return future;
	}

	public CommandFuture<HostInfo> getHostInfo() {
		final CHostInfo info = new CHostInfo();
		final CommandFuture<HostInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new HostInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<InstanceInfo> getInstanceInfo() {
		final CInstanceInfo info = new CInstanceInfo();
		final CommandFuture<InstanceInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new InstanceInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<String> getOfflineMessage(int messageId) {
		final CMessageGet get = new CMessageGet(messageId);
		return executeAndReturnStringProperty(get, "message");
	}

	public CommandFuture<List<Message>> getOfflineMessages() {
		final CMessageList list = new CMessageList();
		final CommandFuture<List<Message>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Message> msg = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					msg.add(new Message(opt));
				}
				future.set(msg);
			}
		});
		return future;
	}

	public CommandFuture<List<AdvancedPermission>> getPermissionAssignments(String permName) {
		final CPermFind find = new CPermFind(permName);
		final CommandFuture<List<AdvancedPermission>> future = new CommandFuture<>();

		query.doCommandAsync(find, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(find, future)) return;

				final List<AdvancedPermission> assignments = new ArrayList<>();
				for (final HashMap<String, String> opt : find.getResponse()) {
					assignments.add(new AdvancedPermission(opt));
				}
				future.set(assignments);
			}
		});
		return future;
	}

	public CommandFuture<Integer> getPermissionIdByName(String permName) {
		final CPermIdGetByName get = new CPermIdGetByName(permName);
		return executeAndReturnIntProperty(get, "permid");
	}

	public CommandFuture<List<AdvancedPermission>> getPermissionOverview(int channelId, int clientDBId) {
		final CPermOverview overview = new CPermOverview(channelId, clientDBId);
		final CommandFuture<List<AdvancedPermission>> future = new CommandFuture<>();

		query.doCommandAsync(overview, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(overview, future)) return;

				final List<AdvancedPermission> permissions = new ArrayList<>();
				for (final HashMap<String, String> opt : overview.getResponse()) {
					permissions.add(new AdvancedPermission(opt));
				}
				future.set(permissions);
			}
		});
		return future;
	}

	public CommandFuture<List<PermissionInfo>> getPermissions() {
		final CPermissionList list = new CPermissionList();
		final CommandFuture<List<PermissionInfo>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<PermissionInfo> permissions = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					permissions.add(new PermissionInfo(opt));
				}
				future.set(permissions);
			}
		});
		return future;
	}

	public CommandFuture<Integer> getPermissionValue(String permName) {
		final CPermGet get = new CPermGet(permName);
		return executeAndReturnIntProperty(get, "permvalue");
	}

	public CommandFuture<List<PrivilegeKey>> getPrivilegeKeys() {
		final CPrivilegeKeyList list = new CPrivilegeKeyList();
		final CommandFuture<List<PrivilegeKey>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<PrivilegeKey> keys = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					keys.add(new PrivilegeKey(opt));
				}
				future.set(keys);
			}
		});
		return future;
	}

	public CommandFuture<List<ServerGroupClient>> getServerGroupClients(int groupId) {
		final CServerGroupClientList list = new CServerGroupClientList(groupId);
		final CommandFuture<List<ServerGroupClient>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<ServerGroupClient> clients = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					clients.add(new ServerGroupClient(opt));
				}
				future.set(clients);
			}
		});
		return future;
	}

	public CommandFuture<List<Permission>> getServerGroupPermissions(int id) {
		final CServerGroupPermList list = new CServerGroupPermList(id);
		final CommandFuture<List<Permission>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<Permission> p = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					p.add(new Permission(opt));
				}
				future.set(p);
			}
		});
		return future;
	}

	public CommandFuture<List<ServerGroup>> getServerGroups() {
		final CServerGroupList list = new CServerGroupList();
		final CommandFuture<List<ServerGroup>> future = new CommandFuture<>();

		query.doCommandAsync(list, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(list, future)) return;

				final List<ServerGroup> groups = new ArrayList<>();
				for (final HashMap<String, String> opt : list.getResponse()) {
					groups.add(new ServerGroup(opt));
				}
				future.set(groups);
			}
		});
		return future;
	}

	public CommandFuture<List<ServerGroup>> getServerGroupsByClientId(int clientDatabaseId) {
		final CServerGroupsByClientId client = new CServerGroupsByClientId(clientDatabaseId);
		final CommandFuture<List<ServerGroup>> future = new CommandFuture<>();

		getServerGroups().onSuccess(new CommandFuture.SuccessListener<List<ServerGroup>>() {
			@Override
			public void handleSuccess(List<ServerGroup> result) {
				final List<ServerGroup> list = new ArrayList<>();
				for (final HashMap<String, String> opt : client.getResponse()) {
					for (final ServerGroup s : result) {
						if (s.getId() == StringUtil.getInt(opt.get("sgid"))) {
							list.add(s);
						}
					}
				}
				future.set(list);
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<Integer> getServerIdByPort(int port) {
		final CServerIdGetByPort s = new CServerIdGetByPort(port);
		return executeAndReturnIntProperty(s, "server_id");
	}

	public CommandFuture<VirtualServerInfo> getServerInfo() {
		final CServerInfo info = new CServerInfo();
		final CommandFuture<VirtualServerInfo> future = new CommandFuture<>();

		query.doCommandAsync(info, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(info, future)) return;
				future.set(new VirtualServerInfo(info.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<Version> getVersion() {
		final CVersion version = new CVersion();
		final CommandFuture<Version> future = new CommandFuture<>();

		query.doCommandAsync(version, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(version, future)) return;
				future.set(new Version(version.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	public CommandFuture<List<VirtualServer>> getVirtualServers() {
		final CServerList serverList = new CServerList();
		final CommandFuture<List<VirtualServer>> future = new CommandFuture<>();

		query.doCommandAsync(serverList, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(serverList, future)) return;

				final ArrayList<VirtualServer> servers = new ArrayList<>();
				for (final HashMap<String, String> opt : serverList.getResponse()) {
					servers.add((new VirtualServer(opt)));
				}
				future.set(servers);
			}
		});
		return future;
	}

	public CommandFuture<Boolean> kickClientFromChannel(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, null, clientIds);
	}

	public CommandFuture<Boolean> kickClientFromChannel(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_CHANNEL, message, clientIds);
	}

	public CommandFuture<Boolean> kickClientFromServer(int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, null, clientIds);
	}

	public CommandFuture<Boolean> kickClientFromServer(String message, int... clientIds) {
		return kickClients(ReasonIdentifier.REASON_KICK_SERVER, message, clientIds);
	}

	private CommandFuture<Boolean> kickClients(ReasonIdentifier reason, String message, int... clientIds) {
		final CClientKick kick = new CClientKick(reason, message, clientIds);
		return executeAndReturnError(kick);
	}

	public CommandFuture<Boolean> login(String username, String password) {
		final CLogin login = new CLogin(username, password);
		return executeAndReturnError(login);
	}

	public CommandFuture<Boolean> logout() {
		final CLogout logout = new CLogout();
		return executeAndReturnError(logout);
	}

	public CommandFuture<Boolean> moveChannel(int channelId, int channelTargetId) {
		return moveChannel(channelId, channelTargetId, -1);
	}

	public CommandFuture<Boolean> moveChannel(int channelId, int channelTargetId, int order) {
		final CChannelMove move = new CChannelMove(channelId, channelTargetId, order);
		return executeAndReturnError(move);
	}

	public CommandFuture<Boolean> moveClient(int clientId, int channelId) {
		return moveClient(clientId, channelId, null);
	}

	public CommandFuture<Boolean> moveClient(int clientId, int channelId, String channelPassword) {
		final CClientMove move = new CClientMove(clientId, channelId, channelPassword);
		return executeAndReturnError(move);
	}

	public CommandFuture<Boolean> moveClient(final int channelId) {
		final CommandFuture<Boolean> future = new CommandFuture<>();

		whoAmI().onSuccess(new CommandFuture.SuccessListener<ServerQueryInfo>() {
			@Override
			public void handleSuccess(ServerQueryInfo result) {
				moveClient(result.getId(), channelId).forwardResult(future);
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<Boolean> moveClient(final int channelId, final String channelPassword) {
		final CommandFuture<Boolean> future = new CommandFuture<>();

		whoAmI().onSuccess(new CommandFuture.SuccessListener<ServerQueryInfo>() {
			@Override
			public void handleSuccess(ServerQueryInfo result) {
				moveClient(result.getId(), channelId, channelPassword).forwardResult(future);
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<Boolean> pokeClient(int clientId, String message) {
		final CClientPoke poke = new CClientPoke(clientId, message);
		return executeAndReturnError(poke);
	}

	/**
	 * Leaves the TeamSpeak 3 server.
	 *
	 * @return True if succesful, otherwise false.
	 */
	public CommandFuture<Boolean> quit() {
		final CQuit quit = new CQuit();
		return executeAndReturnError(quit);
	}

	public void registerAllEvents() {
		// Technically not 100% async
		final int channelId = whoAmI().get().getChannelId();

		registerEvent(TS3EventType.CHANNEL, channelId);
		registerEvent(TS3EventType.SERVER);
		registerEvent(TS3EventType.TEXT_CHANNEL);
		registerEvent(TS3EventType.TEXT_PRIVATE);
		registerEvent(TS3EventType.TEXT_SERVER);
	}

	public CommandFuture<Boolean> registerEvent(TS3EventType t) {
		return registerEvent(t, -1);
	}

	public CommandFuture<Boolean> registerEvent(TS3EventType t, int channelId) {
		final CServerNotifyRegister r = new CServerNotifyRegister(t, channelId);
		return executeAndReturnError(r);
	}

	public void registerEvents(TS3EventType... t) {
		for (final TS3EventType type : t) {
			registerEvent(type, -1);
		}
	}

	public CommandFuture<Boolean> removeClientFromServerGroup(int groupId, int clientDatabaseId) {
		final CServerGroupDelClient del = new CServerGroupDelClient(groupId, clientDatabaseId);
		return executeAndReturnError(del);
	}

	public void removeTS3Listeners(TS3Listener... l) {
		query.getEventManager().removeListeners(l);
	}

	public CommandFuture<Boolean> renameChannelGroup(int groupId, String name) {
		final CChannelGroupRename rename = new CChannelGroupRename(groupId, name);
		return executeAndReturnError(rename);
	}

	public CommandFuture<Boolean> renameServerGroup(int id, String name) {
		final CServerGroupRename rename = new CServerGroupRename(id, name);
		return executeAndReturnError(rename);
	}

	/**
	 * Resets all permissions and deletes all server/channel groups. Use carefully.
	 *
	 * @return A new administrator account
	 */
	public CommandFuture<String> resetPermissions() {
		final CPermReset reset = new CPermReset();
		return executeAndReturnStringProperty(reset, "token");
	}

	public CommandFuture<Boolean> selectVirtualServerById(int id) {
		final CUse use = new CUse(id, -1);
		return executeAndReturnError(use);
	}

	public CommandFuture<Boolean> selectVirtualServerByPort(int port) {
		final CUse use = new CUse(-1, port);
		return executeAndReturnError(use);
	}

	public CommandFuture<Boolean> selectVirtualServer(VirtualServer server) {
		return selectVirtualServerById(server.getId());
	}

	/**
	 * Sends an offline message to a Client
	 *
	 * @param clientUId
	 * 		The Unique string
	 * @param subject
	 * 		header
	 * @param message
	 * 		your message to send
	 *
	 * @return true if the message was send
	 */
	public CommandFuture<Boolean> sendOfflineMessage(String clientUId, String subject, String message) {
		final CMessageAdd add = new CMessageAdd(clientUId, subject, message);
		return executeAndReturnError(add);
	}

	/**
	 * Sends a TextMessage
	 *
	 * @param targetMode
	 * 		The targetmode (Server, channel or private)
	 * @param targetId
	 * 		The recipient of this message
	 * @param message
	 * 		Yout text message to send
	 *
	 * @return true if the message was send
	 */
	public CommandFuture<Boolean> sendTextMessage(TextMessageTargetMode targetMode, int targetId, String message) {
		final CSendTextMessage msg = new CSendTextMessage(targetMode.getIndex(), targetId, message);
		return executeAndReturnError(msg);
	}

	/**
	 * Sends a ChannelMessage
	 *
	 * @param channelId
	 * 		The channelID which receive your message
	 * @param message
	 * 		Your message which you would send
	 *
	 * @return true if the message was succesfully send
	 */
	public CommandFuture<Boolean> sendChannelMessage(int channelId, String message) {
		return sendTextMessage(TextMessageTargetMode.CHANNEL, channelId, message);
	}

	public CommandFuture<Boolean> sendChannelMessage(final String message) {
		final CommandFuture<Boolean> future = new CommandFuture<>();

		whoAmI().onSuccess(new CommandFuture.SuccessListener<ServerQueryInfo>() {
			@Override
			public void handleSuccess(ServerQueryInfo result) {
				sendChannelMessage(result.getChannelId(), message).forwardResult(future);
			}
		}).forwardFailure(future);
		return future;
	}

	public CommandFuture<Boolean> sendServerMessage(int serverId, String message) {
		return sendTextMessage(TextMessageTargetMode.SERVER, serverId, message);
	}

	public CommandFuture<Boolean> sendServerMessage(String message) {
		return sendServerMessage(1, message);
	}

	public CommandFuture<Boolean> sendPrivateMessage(int clientId, String message) {
		return sendTextMessage(TextMessageTargetMode.CLIENT, clientId, message);
	}

	public CommandFuture<Boolean> setClientChannelGroup(int groupId, int channelId, int clientDBId) {
		final CSetClientChannelGroup group = new CSetClientChannelGroup(groupId, channelId, clientDBId);
		return executeAndReturnError(group);
	}

	public CommandFuture<Boolean> setMessageRead(int messageId) {
		return setMessageReadFlag(messageId, true);
	}

	public CommandFuture<Boolean> setMessageReadFlag(int messageId, boolean read) {
		final CMessageUpdateFlag flag = new CMessageUpdateFlag(messageId, read);
		return executeAndReturnError(flag);
	}

	public CommandFuture<Boolean> setNickname(String name) {
		final HashMap<ClientProperty, String> options = new HashMap<>();
		options.put(ClientProperty.CLIENT_NICKNAME, name);
		return updateClient(options);
	}

	public CommandFuture<Boolean> startServer(int id) {
		final CServerStart start = new CServerStart(id);
		return executeAndReturnError(start);
	}

	public CommandFuture<Boolean> stopServer(int id) {
		final CServerStop stop = new CServerStop(id);
		return executeAndReturnError(stop);
	}

	public CommandFuture<Boolean> stopServerProcess() {
		final CServerProcessStop stop = new CServerProcessStop();
		return executeAndReturnError(stop);
	}

	public CommandFuture<Boolean> unregisterAllEvents() {
		final CServerNotifyUnregister unr = new CServerNotifyUnregister();
		return executeAndReturnError(unr);
	}

	public CommandFuture<Boolean> updateClient(HashMap<ClientProperty, String> options) {
		final CClientUpdate update = new CClientUpdate(options);
		return executeAndReturnError(update);
	}

	public CommandFuture<Boolean> usePrivilegeKey(String token) {
		final CPrivilegeKeyUse use = new CPrivilegeKeyUse(token);
		return executeAndReturnError(use);
	}

	public CommandFuture<ServerQueryInfo> whoAmI() {
		final CWhoAmI whoAmI = new CWhoAmI();
		final CommandFuture<ServerQueryInfo> future = new CommandFuture<>();

		query.doCommandAsync(whoAmI, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(whoAmI, future)) return;
				future.set(new ServerQueryInfo(whoAmI.getFirstResponse().getMap()));
			}
		});
		return future;
	}

	private CommandFuture<Boolean> executeAndReturnError(final Command command) {
		final CommandFuture<Boolean> future = new CommandFuture<>();

		query.doCommandAsync(command, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(command, future)) return;
				future.set(true);
			}
		});
		return future;
	}

	private CommandFuture<String> executeAndReturnStringProperty(final Command command, final String property) {
		final CommandFuture<String> future = new CommandFuture<>();

		query.doCommandAsync(command, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(command, future)) return;
				future.set(command.getFirstResponse().get(property));
			}
		});
		return future;
	}

	private CommandFuture<Integer> executeAndReturnIntProperty(final Command command, final String property) {
		final CommandFuture<Integer> future = new CommandFuture<>();

		query.doCommandAsync(command, new Callback() {
			@Override
			public void handle() {
				if (hasFailed(command, future)) return;
				future.set(StringUtil.getInt(command.getFirstResponse().get(property)));
			}
		});
		return future;
	}

	private boolean hasFailed(Command command, CommandFuture<?> future) {
		final QueryError error = command.getError();
		if (error.isSuccessful()) return false;

		final TS3CommandFailedException exception = new TS3CommandFailedException(error);
		future.fail(exception);
		return true;
	}
}
