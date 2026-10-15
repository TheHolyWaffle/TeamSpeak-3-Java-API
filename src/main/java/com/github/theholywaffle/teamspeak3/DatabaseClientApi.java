package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientIdentity;
import com.github.theholywaffle.teamspeak3.api.wrapper.CustomPropertyAssignment;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClient;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Administration of clients known to the virtual server's database, including custom properties.
 *
 * @see DatabaseClientApiAsync the asynchronous form
 */
public final class DatabaseClientApi {

    private final DatabaseClientApiAsync async;

    DatabaseClientApi(DatabaseClientApiAsync async) {
        this.async = async;
    }

    /**
     * Lists all database clients, fetching them in pages of 200.
     *
     * @return all database clients
     */
    public List<DatabaseClient> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * Lists one page of database clients.
     *
     * @param offset the index of the first client to return
     * @param count the maximum number of clients to return
     * @return the database clients in this page
     */
    public List<DatabaseClient> list(int offset, int count) {
        return CommandFutures.join(async.list(offset, count));
    }

    /**
     * Gets detailed information about a database client.
     *
     * @param databaseId the client database ID
     * @return the client information
     * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 512 if the client does not exist
     */
    public DatabaseClientInfo info(ClientDatabaseId databaseId) {
        return CommandFutures.join(async.info(databaseId));
    }

    /**
     * Finds database clients whose last nickname matches a pattern.
     *
     * @param nicknamePattern the nickname pattern; {@code %} matches any text
     * @return the matching clients' information; empty if none match
     */
    public List<DatabaseClientInfo> search(String nicknamePattern) {
        return CommandFutures.join(async.search(nicknamePattern));
    }

    /**
     * Finds a database client by unique identifier.
     *
     * @param uniqueId the client's unique identifier
     * @return the client information, or empty if the database has no such client
     */
    public Optional<DatabaseClientInfo> findByUniqueId(String uniqueId) {
        return CommandFutures.join(async.findByUniqueId(uniqueId));
    }

    /**
     * Looks up a last-known nickname and database identity in one query, even for an offline client.
     * Use {@link #findByUniqueId(String)} when full database information is needed.
     *
     * @param uniqueId the nonempty client unique identifier
     * @return the identity, or empty if the server does not know the client
     */
    public Optional<ClientIdentity> findIdentityByUniqueId(String uniqueId) {
        return CommandFutures.join(async.findIdentityByUniqueId(uniqueId));
    }

    /**
     * Looks up a last-known nickname and unique identifier in one query, even for an offline client.
     *
     * @param databaseId the client database ID
     * @return the identity, or empty if the server does not know the client
     */
    public Optional<ClientIdentity> findIdentityByDatabaseId(ClientDatabaseId databaseId) {
        return CommandFutures.join(async.findIdentityByDatabaseId(databaseId));
    }

    /**
     * Changes properties of a database client.
     *
     * @param databaseId the client database ID
     * @param properties the properties to change
     */
    public void edit(ClientDatabaseId databaseId, Map<ClientProperty, String> properties) {
        CommandFutures.join(async.edit(databaseId, properties));
    }

    /**
     * Deletes a client and its properties from the database.
     *
     * @param databaseId the client database ID
     */
    public void delete(ClientDatabaseId databaseId) {
        CommandFutures.join(async.delete(databaseId));
    }

    /**
     * Gets a database client's custom properties.
     *
     * @param databaseId the client database ID
     * @return an unmodifiable map of property keys to values
     */
    public Map<String, String> customProperties(ClientDatabaseId databaseId) {
        return CommandFutures.join(async.customProperties(databaseId));
    }

    /**
     * Creates or replaces one custom property.
     *
     * @param databaseId the client database ID
     * @param key the property key
     * @param value the property value
     */
    public void setCustomProperty(ClientDatabaseId databaseId, String key, String value) {
        CommandFutures.join(async.setCustomProperty(databaseId, key, value));
    }

    /**
     * Creates or replaces several custom properties, one command per entry.
     *
     * @param databaseId the client database ID
     * @param properties the property keys and values
     */
    public void setCustomProperties(ClientDatabaseId databaseId, Map<String, String> properties) {
        CommandFutures.join(async.setCustomProperties(databaseId, properties));
    }

    /**
     * Removes one custom property.
     *
     * @param databaseId the client database ID
     * @param key the property key
     */
    public void deleteCustomProperty(ClientDatabaseId databaseId, String key) {
        CommandFutures.join(async.deleteCustomProperty(databaseId, key));
    }

    /**
     * Finds custom property assignments by key and value pattern.
     *
     * @param key the property key
     * @param valuePattern the value pattern; {@code %} matches any value
     * @return the matching assignments; empty if none match
     */
    public List<CustomPropertyAssignment> searchCustomProperty(String key, String valuePattern) {
        return CommandFutures.join(async.searchCustomProperty(key, valuePattern));
    }
}
