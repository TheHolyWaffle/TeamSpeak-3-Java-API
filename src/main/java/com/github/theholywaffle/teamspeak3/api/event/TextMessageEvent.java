package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.util.Optional;

public class TextMessageEvent extends BaseEvent {

    public TextMessageEvent(Wrapper wrapper) {
        super(wrapper);
    }

    /** @return the target mode, or {@link TextMessageTargetMode#UNKNOWN} for a mode this library does not know */
    public TextMessageTargetMode getTargetMode() {
        return requireCode("targetmode", TextMessageTargetMode::fromIndex);
    }

    public String getMessage() {
        return require("msg");
    }

    /**
     * Gets the client ID of the recipient of a private message.
     * <ul>
     * <li>If the private message was sent <b>to</b> the query, the target ID will be the query's client ID</li>
     * <li>If the private message was sent <b>by</b> the query, the target ID will be the recipient's client ID</li>
     * <li>If this is not an event for a private message, this method returns an empty optional</li>
     * </ul>
     *
     * @return the client ID of the recipient
     */
    public Optional<ClientId> getTargetClientId() {
        return findClientId("target");
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onTextMessage(this);
    }
}
