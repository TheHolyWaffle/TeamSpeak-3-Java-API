package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;

public class InstanceInfo extends Wrapper {

    public InstanceInfo(Map<String, String> map) {
        super(map);
    }

    public int getDatabaseVersion() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_DATABASE_VERSION);
    }

    public int getFileTransferPort() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_FILETRANSFER_PORT);
    }

    /** @return the limit in bytes per second, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getMaxDownloadBandwidth() {
        return requireLimit(ServerInstanceProperty.SERVERINSTANCE_MAX_DOWNLOAD_TOTAL_BANDWIDTH);
    }

    /** @return the limit in bytes per second, or empty if unlimited (TeamSpeak sends 2<sup>64</sup>-1) */
    public OptionalLong getMaxUploadBandwidth() {
        return requireLimit(ServerInstanceProperty.SERVERINSTANCE_MAX_UPLOAD_TOTAL_BANDWIDTH);
    }

    public int getGuestServerQueryGroup() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_GUEST_SERVERQUERY_GROUP);
    }

    public int getMaxFloodCommands() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_SERVERQUERY_FLOOD_COMMANDS);
    }

    public Duration getMaxFloodTime() {
        return requireSeconds(ServerInstanceProperty.SERVERINSTANCE_SERVERQUERY_FLOOD_TIME);
    }

    public Duration getFloodBanTime() {
        return requireSeconds(ServerInstanceProperty.SERVERINSTANCE_SERVERQUERY_BAN_TIME);
    }

    public int getServerAdminGroup() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_TEMPLATE_SERVERADMIN_GROUP);
    }

    public int getDefaultServerGroup() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_TEMPLATE_SERVERDEFAULT_GROUP);
    }

    public int getChannelAdminGroup() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_TEMPLATE_CHANNELADMIN_GROUP);
    }

    public int getDefaultChannelGroup() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_TEMPLATE_CHANNELDEFAULT_GROUP);
    }

    public int getPermissionsVersion() {
        return requireInt(ServerInstanceProperty.SERVERINSTANCE_PERMISSIONS_VERSION);
    }
}
