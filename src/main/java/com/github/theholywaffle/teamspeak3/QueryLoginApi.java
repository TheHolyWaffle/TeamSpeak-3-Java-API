package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.CreatedQueryLogin;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryLogin;

import java.util.List;

/**
 * ServerQuery login administration.
 *
 * @see QueryLoginApiAsync the asynchronous form
 */
public final class QueryLoginApi {

	private final QueryLoginApiAsync async;

	QueryLoginApi(QueryLoginApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists all ServerQuery logins.
	 *
	 * @return the logins
	 */
	public List<QueryLogin> list() {
		return CommandFutures.join(async.list());
	}

	/**
	 * Lists ServerQuery logins whose name matches a pattern.
	 *
	 * @param loginNamePattern the login name pattern; {@code %} matches any text
	 * @return the matching logins; empty if none match
	 */
	public List<QueryLogin> search(String loginNamePattern) {
		return CommandFutures.join(async.search(loginNamePattern));
	}

	/**
	 * Creates a ServerQuery login with a new query client identity, which is not tied to a
	 * virtual server and can be used instance-wide.
	 *
	 * @param loginName the login name
	 * @return the login, its new client database ID and its generated password
	 */
	public CreatedQueryLogin create(String loginName) {
		return CommandFutures.join(async.create(loginName));
	}

	/**
	 * Creates a ServerQuery login for an existing database client of the selected virtual server.
	 *
	 * @param loginName the login name
	 * @param clientDatabaseId the client database ID
	 * @return the login and its generated password
	 */
	public CreatedQueryLogin create(String loginName, ClientDatabaseId clientDatabaseId) {
		return CommandFutures.join(async.create(loginName, clientDatabaseId));
	}

	/**
	 * Deletes the ServerQuery login of a database client.
	 *
	 * @param clientDatabaseId the client database ID
	 */
	public void delete(ClientDatabaseId clientDatabaseId) {
		CommandFutures.join(async.delete(clientDatabaseId));
	}

	/**
	 * Creates or replaces the ServerQuery login of this query client's identity.
	 *
	 * @param loginName the login name
	 * @return the generated password
	 */
	public String updateOwn(String loginName) {
		return CommandFutures.join(async.updateOwn(loginName));
	}
}
