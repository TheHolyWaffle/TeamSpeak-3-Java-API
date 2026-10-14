package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class BaseEvent extends Wrapper implements TS3Event {

    protected BaseEvent(Wrapper wrapper) {
        super(wrapper);
    }

    /**
     * Gets the ID of the client who caused this event to happen.
     * <p>
     * Because not all events are caused by clients, not all events have an invoker.<br>
     * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
     * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
     * quit or changed channels themselves. These values are only present if another client
     * caused the event to happen by moving, kicking or banning a client.
     * </p><p>
     * Client ID {@code 0} identifies the server itself, for example when creating a channel moves its creator.
     * </p>
     *
     * @return the client ID of the invoker, or empty if the event does not contain an invoker
     */
    public Optional<ClientId> getInvokerId() {
        return findClientId("invokerid");
    }

    /**
     * Gets the nickname of the client who caused this event to happen.
     * <p>
     * Because not all events are caused by clients, not all events have an invoker.<br>
     * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
     * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
     * quit or changed channels themselves. These values are only present if another client
     * caused the event to happen by moving, kicking or banning a client.
     * </p>
     *
     * @return the nickname of the invoker, or empty if the event does not contain an invoker
     */
    public Optional<String> getInvokerName() {
        return find("invokername");
    }

    /**
     * Gets the unique identifier of the client who caused this event to happen.
     * <p>
     * Because not all events are caused by clients, not all events have an invoker.<br>
     * Most importantly, client events (i.e. {@link ClientJoinEvent}, {@link ClientLeaveEvent}
     * {@link ClientMovedEvent}) do <b>not</b> have an invoker if a client joined,
     * quit or changed channels themselves. These values are only present if another client
     * caused the event to happen by moving, kicking or banning a client.
     * </p><p>
     * The server as invoker has no unique ID.
     * </p>
     *
     * @return the unique ID of the invoker, or empty if the event does not contain one
     */
    public Optional<String> getInvokerUniqueId() {
        return find("invokeruid");
    }

    /**
     * Gets the reason ID of the cause of this event.
     * <p>
     * Here's an incomplete list of known reason IDs:
     * </p>
     * <table>
     * <caption>Incomplete list of known reason IDs</caption>
     * <tr><th>ID</th><th>Description</th></tr>
     * <tr><td>0</td><td>No external cause</td></tr>
     * <tr><td>1</td><td>Client was moved (by other client / by creating a new channel)</td></tr>
     * <tr><td>3</td><td>Client timed out</td></tr>
     * <tr><td>4</td><td>Client kicked from a channel or channel deleted</td></tr>
     * <tr><td>5</td><td>Client kicked from the server</td></tr>
     * <tr><td>6</td><td>Client banned from the server</td></tr>
     * <tr><td>8</td><td>Client left the server (no external cause)</td></tr>
     * <tr><td>10</td><td>Virtual server or channel edited</td></tr>
     * <tr><td>11</td><td>Server shut down</td></tr>
     * </table>
     *
     * @return the reason ID for this event, or empty if the event does not contain one
     */
    public OptionalInt getReasonId() {
        return findInt("reasonid");
    }

    /**
     * Gets the reason for this event.
     * This field is rarely present, even if the reason ID is non-zero.
     * <p>
     * In case of a client getting kicked or banned, this will return the user-provided reason.
     * </p>
     *
     * @return the reason for this event, or empty if no reason message is present
     */
    public Optional<String> getReasonMessage() {
        return find("reasonmsg");
    }
}
