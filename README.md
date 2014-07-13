TeamSpeak 3 Java API
====================

An Java 7 implementation of [TeamSpeak's 3 server query API](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf).


## Features

- Contains all server query functionality! (see [TeamSpeak 3 Server Query Manual](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf))
- Built-in keep alive method
- Threaded event-based system
- No extra libraries

## Getting Started

**Download**

- Standalone Jar: You can download the latest release [here](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/tree/mvn-repo/com/github/theholywaffle/teamspeak3-api) and add it to the buildpath of your project. 

or

- Maven: add the following to your pom.xml

```xml
<project>	
	...
	<repositories>
		...
		<repository>
			<id>TeamSpeak-3-Java-API-mvn-repo</id>
			<url>https://raw.githubusercontent.com/TheHolyWaffle/TeamSpeak-3-Java-API/mvn-repo/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
		</repository>
		...
	</repositories>
	
	<dependencies>
		...
		<dependency>
			<groupId>com.github.theholywaffle</groupId>
			<artifactId>teamspeak3-api</artifactId>
			<version>[1.0.0,2.0.0)</version>
		</dependency>
		...
	</dependencies>
	...
</project>
```

**Usage**

All functionality is contained in the [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.

1. Create a [TS3Config](src/main/java/com/github/theholywaffle/teamspeak3/TS3Config.java) object and customize it.
2. Create a [TS3Query](src/main/java/com/github/theholywaffle/teamspeak3/TS3Query.java) object with your TS3Config as argument.
3. Call `TS3Query#connect()` to connect to the server.
4. Call `TS3Query#getApi()` to get an [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.
5. Do whatever you want with this api :)


**Example:**

```java
final TS3Config config = new TS3Config();
config.setHost("77.77.77.77");
config.setDebugLevel(Level.ALL);
config.setLoginCredentials("serveradmin", "serveradminpassword");

final TS3Query query = new TS3Query(config);
query.connect();
    
final TS3Api api = query.getApi();
api.selectVirtualServerById(1);
api.setNickname("PutPutBot");
api.sendChannelMessage("PutPutBot is online!");
...
```
    
**More examples:**

[look here](example)

**Latest Javadocs:**

[look here](http://theholywaffle.github.io/TeamSpeak-3-Java-API/latest/)
    
**Important:**

Only use `FloodRate.UNLIMITED` if you are sure that your query account is whitelisted. If not, use `FloodRate.DEFAULT`. The server will temporarily ban your account if you send too many commands in a short period of time. For more info on this check [TeamSpeak 3 Server Query Manual, page 6](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf#page=6).

**TS3Config Settings**

|Option | Description | Default value | Required |
|--- | --- |:---:|:---:|
|Host/IP | IP/Host of TeamSpeak 3 server.|  | yes |
|QueryPort | Query port of TeamSpeak 3 server. | 10011 | yes |
|FloodRate | Prevents possible spam to the server. | `FloodRate.DEFAULT` | no |
|Username | Username of your query account. |   | no |
|Password | Password of your query account. |  | no |
|DebugLevel | Determines how much will be logged. | `Level.WARNING` | no |
|Debug to file | Write logs to logfile (teamspeak.log).|  False | no |

## TODO

* Add Javadoc to [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java).

## Questions or bugs?

Please let me know them [here](../../issues). I'll help you out as soon as I can.
