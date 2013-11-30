TeamSpeak 3 Java API
====================

An Java implementation of [TeamSpeak's 3 server query API](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf).


## Features

- Contains all functionality! (see [TeamSpeak 3 Server Query Manual](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf))
- Built-in keep alive method
- Threaded event-based system
- No extra libraries

## Getting Started

First of all, you can always download the latest release [here](../../releases/latest). And add it to the buildpath of your project.

All functionality is contained in the [TS3Api](src/com/github/theholywaffle/teamspeak3/TS3Api.java) object.

1. In order to obtain this api object you first need to create a new [TS3Query](src/com/github/theholywaffle/teamspeak3/TS3Query.java) object with the correct constructor parameters.
2. (Optional) Enable debug if you wish by calling `debug(Level l)`. Default Level is `Level.WARNING`. Use `Level.ALL` to debug everything.
3. Connect to the server with `connect()`
4. Call `getApi()` to get an TS3Api object.
5. Do whatever you want with this api :)

**Example:**

    TS3Api api = new TS3Query("77.77.77.77", TS3Query.DEFAULT_PORT,FloodRate.DEFAULT).debug(Level.ALL).connect().getApi();
    api.login("serveradmin", "serveradminpassword"); //Your ServerQuery credentials
    api.selectDefaultVirtualServer();
    api.setNickname("PutPutBot");
    ...
    
More examples can be found [here](src/com/github/theholywaffle/teamspeak3/example).
    
**Important:**

Only use `FloodRate.UNLIMITED` if you are sure that your query account is whitelisted. If not, use `FloodRate.DEFAULT`. The server will temporarily ban your account if you send too many commands in a short period of time. For more info on this check [TeamSpeak 3 Server Query Manual, page 6](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf).

## TODO (aka I need your help on this)

* Add Javadoc to [TS3Api](src/com/github/theholywaffle/teamspeak3/TS3Api.java).
* Add more methods to simplify [TS3Api](src/com/github/theholywaffle/teamspeak3/TS3Api.java).

## Questions or bugs?

Please let me know them [here](../../issues). I'll help you out as soon as I can.