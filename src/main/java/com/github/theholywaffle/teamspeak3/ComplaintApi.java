package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Complaint;
import java.util.List;

/**
 * Complaint administration.
 *
 * @see ComplaintApiAsync the asynchronous form
 */
public final class ComplaintApi {

    private final ComplaintApiAsync async;

    ComplaintApi(ComplaintApiAsync async) {
        this.async = async;
    }

    /**
     * Lists all complaints.
     *
     * @return the complaints
     */
    public List<Complaint> list() {
        return CommandFutures.join(async.list());
    }

    /**
     * Lists complaints about one database client.
     *
     * @param targetDatabaseId the database ID of the client complained about
     * @return the complaints; empty if there are none
     */
    public List<Complaint> about(ClientDatabaseId targetDatabaseId) {
        return CommandFutures.join(async.about(targetDatabaseId));
    }

    /**
     * Files a complaint about a database client.
     *
     * @param targetDatabaseId the database ID of the client to complain about
     * @param message the complaint message
     */
    public void add(ClientDatabaseId targetDatabaseId, String message) {
        CommandFutures.join(async.add(targetDatabaseId, message));
    }

    /**
     * Deletes one complaint.
     *
     * @param targetDatabaseId the database ID of the client complained about
     * @param fromDatabaseId the database ID of the complaining client
     */
    public void delete(ClientDatabaseId targetDatabaseId, ClientDatabaseId fromDatabaseId) {
        CommandFutures.join(async.delete(targetDatabaseId, fromDatabaseId));
    }

    /**
     * Deletes all complaints about a database client.
     *
     * @param targetDatabaseId the database ID of the client complained about
     */
    public void deleteAll(ClientDatabaseId targetDatabaseId) {
        CommandFutures.join(async.deleteAll(targetDatabaseId));
    }
}
