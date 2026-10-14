package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class DatabaseClientInfo extends DatabaseClient {

    public DatabaseClientInfo(Map<String, String> map) {
        super(map);
    }

    @Override
    public ClientDatabaseId getDatabaseId() {
        return requireDatabaseId(ClientProperty.CLIENT_DATABASE_ID);
    }

    /** @return the avatar hash, or empty if the client has no avatar */
    public Optional<String> getAvatar() {
        return findText(ClientProperty.CLIENT_FLAG_AVATAR);
    }

    public long getMonthlyBytesUploaded() {
        return requireLong(ClientProperty.CLIENT_MONTH_BYTES_UPLOADED);
    }

    public long getMonthlyBytesDownloaded() {
        return requireLong(ClientProperty.CLIENT_MONTH_BYTES_DOWNLOADED);
    }

    public long getTotalBytesUploaded() {
        return requireLong(ClientProperty.CLIENT_TOTAL_BYTES_UPLOADED);
    }

    public long getTotalBytesDownloaded() {
        return requireLong(ClientProperty.CLIENT_TOTAL_BYTES_DOWNLOADED);
    }

    /** @return the icon ID, or empty if the server omits it (TeamSpeak 3.13 does) */
    public OptionalLong getIconId() {
        return findLong(ClientProperty.CLIENT_ICON_ID);
    }

    /** @return the base64 hash of the unique ID, or empty if the server sends none */
    public Optional<String> getBase64HashClientUID() {
        return findText("client_base64HashClientUID");
    }
}
