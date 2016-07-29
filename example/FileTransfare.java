import java.util.logging.Level;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;

public class FileTransfare {
	public static void main(String[] args) {
		final TS3Config config = new TS3Config();
		config.setHost(System.getProperty("tsHost"));
		config.setDebugLevel(Level.ALL);

		final TS3Query query = new TS3Query(config);
		query.connect();

		final TS3Api api = query.getApi();
		api.login(System.getProperty("tsUsername"), System.getProperty("tsPassword"));
		api.selectVirtualServerById(Integer.getInteger("tsServerId", 1));
		api.setNickname(System.getProperty("tsNickname", "Filebot"));
		
		api.initFileDownload("/icon_1002");
	}
}
