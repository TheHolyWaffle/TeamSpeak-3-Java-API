TeamSpeak 3 Java API
====================
[![Build Status](https://travis-ci.org/TheHolyWaffle/TeamSpeak-3-Java-API.svg)](https://travis-ci.org/TheHolyWaffle/TeamSpeak-3-Java-API) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

A Java 7 implementation of the [TeamSpeak 3 Server Query API](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf).


## Features

- [Documented source](http://theholywaffle.github.io/TeamSpeak-3-Java-API/)
- Contains all server query functionality! (see [TeamSpeak 3 Server Query Manual](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf))
- Built-in keep alive method
- Threaded event-based system
- Both [synchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) and [asynchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3ApiAsync.java) implementations available
- No extra libraries

## Getting Started

### Download

- **Option 1 (Standalone Jar)**: 

   Download the <a href="http://theholywaffle.github.io/TeamSpeak-3-Java-API/download.html" target="_blank">latest release</a> and add it to the buildpath of your project. (<a href="http://theholywaffle.github.io/TeamSpeak-3-Java-API/download-sources.html" target="_blank">sources-jar</a> | <a href="http://theholywaffle.github.io/TeamSpeak-3-Java-API/download-javadoc.html" target="_blank">javadoc-jar</a>)

- **Option 2 (Maven)**: 

   Add the following to your pom.xml

```xml
<project>	
	
	<!-- other settings -->
	
	<repositories>
		<repository>
			<id>TeamSpeak-3-Java-API-mvn-repo</id>
			<url>https://raw.githubusercontent.com/TheHolyWaffle/TeamSpeak-3-Java-API/mvn-repo/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
		</repository>
	</repositories>
	
	<dependencies>
		<dependency>
			<groupId>com.github.theholywaffle</groupId>
			<artifactId>teamspeak3-api</artifactId>
			<version>[1.0.0,2.0.0)</version>
		</dependency>		
	</dependencies>

</project>
```

### Usage

All functionality is contained in the [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.

1. Create a [TS3Config](src/main/java/com/github/theholywaffle/teamspeak3/TS3Config.java) object and customize it.
2. Create a [TS3Query](src/main/java/com/github/theholywaffle/teamspeak3/TS3Query.java) object with your TS3Config as argument.
3. Call `TS3Query#connect()` to connect to the server.
4. Call `TS3Query#getApi()` to get an [TS3Api](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) object.
5. Do whatever you want with this api :)


### Example

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
    
### More examples

[here](example)

### Javadocs

[here](http://theholywaffle.github.io/TeamSpeak-3-Java-API/latest/)

### Important

Only use `FloodRate.UNLIMITED` if you are sure that your query account is whitelisted. If not, use `FloodRate.DEFAULT`. The server will temporarily ban your account if you send too many commands in a short period of time. For more info on this, check the [TeamSpeak 3 Server Query Manual, page 6](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf#page=6).

### TS3Config Settings

|Option | Description | Default value | Required |
|--- | --- |:---:|:---:|
|Host/IP | IP/Host of TeamSpeak 3 server.|  | yes |
|QueryPort | Query port of TeamSpeak 3 server. | 10011 | yes |
|FloodRate | Prevents possible spam to the server. | `FloodRate.DEFAULT` | no |
|Username | Username of your query account. |   | no |
|Password | Password of your query account. |  | no |
|DebugLevel | Determines how much will be logged. | `Level.WARNING` | no |
|Debug to file | Write logs to logfile (teamspeak.log). | False | no |
|Command timeout | Time until a command waiting for a response fails | 4000 (ms) | no |

## Building this project

We're using Maven to automate the build process.

##### Prerequisites:
- Java Development Kit (JDK) 7 or higher
- [Maven 2 or 3](https://maven.apache.org/download.cgi)

##### Compiling:
- Check out this repository
- Run `mvn clean install` in the project directory

This will create 3 JAR files in the `target` directory: The compiled project, the sources, and the javadocs.

## TODO

* Add Javadoc to core API and wrapper classes.

## Questions or bugs?

Please let us know [here](../../issues). We'll try to help you as soon as we can.

If you just have a simple question or want to talk to us about something else, feel free to join the [repository chat](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API) on Gitter.
