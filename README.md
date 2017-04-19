TeamSpeak 3 Java API
====================
[![Build Status](https://travis-ci.org/TheHolyWaffle/TeamSpeak-3-Java-API.svg)](https://travis-ci.org/TheHolyWaffle/TeamSpeak-3-Java-API) [![Maven Central](https://img.shields.io/maven-central/v/com.github.theholywaffle/teamspeak3-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.theholywaffle%22%20a%3A%22teamspeak3-api%22) [![Javadocs](http://www.javadoc.io/badge/com.github.theholywaffle/teamspeak3-api.svg)](http://www.javadoc.io/doc/com.github.theholywaffle/teamspeak3-api) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

A Java 7 wrapper of the [TeamSpeak 3](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf)  Server Query API


## Features

- Contains almost all server query functionality! (see [TeamSpeak 3 Server Query Manual](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf))
- Built-in keep alive method
- Threaded event-based system
- Both [synchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3Api.java) and [asynchronous](src/main/java/com/github/theholywaffle/teamspeak3/TS3ApiAsync.java) implementations available
- Can be set up to reconnect and automatically resume execution after a connection problem

## Getting Started

### Download

- **Option 1 (Standalone Jar)**: 

   Download the [latest release](https://search.maven.org/remote_content?g=com.github.theholywaffle&a=teamspeak3-api&v=LATEST&c=with-dependencies) and add this jar to the buildpath of your project.

- **Option 2 (Maven)**: 

   Add the following to your pom.xml

```xml
<dependency>
	<groupId>com.github.theholywaffle</groupId>
	<artifactId>teamspeak3-api</artifactId>
	<version>...</version>
</dependency>
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

final TS3Query query = new TS3Query(config);
query.connect();

final TS3Api api = query.getApi();
api.login("serveradmin", "serveradminpassword");
api.selectVirtualServerById(1);
api.setNickname("PutPutBot");
api.sendChannelMessage("PutPutBot is online!");
...
```

### More examples

[here](example)

## Extra notes

### FloodRate

Only use `FloodRate.UNLIMITED` if you are sure that your query account is whitelisted. If not, use `FloodRate.DEFAULT`. The server will temporarily ban your account if you send too many commands in a short period of time. For more info on this, check the [TeamSpeak 3 Server Query Manual, page 6](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf#page=6).

### TS3Config Settings

|Option | Description | Default value | Required |
|--- | --- |:---:|:---:|
|Host/IP | IP/Host of TeamSpeak 3 server.|  | yes |
|QueryPort | Query port of TeamSpeak 3 server. | 10011 | no |
|FloodRate | Prevents possible spam to the server. | `FloodRate.DEFAULT` | no |
|Command timeout | Time until a command waiting for a response fails | 4000 (ms) | no |

## Questions or bugs?

Please let us know [here](../../issues). We'll try to help you as soon as we can.

If you just have a simple question or want to talk to us about something else, feel free to join the [repository chat](https://gitter.im/TheHolyWaffle/TeamSpeak-3-Java-API) on Gitter.
