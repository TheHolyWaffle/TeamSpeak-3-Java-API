TeamSpeak 3 Java API
====================

An Java implementation of [TeamSpeak's 3 server query API](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf).


Features
========

- Contains all functionality (see [TeamSpeak 3 Server Query Manual](http://media.teamspeak.com/ts3_literature/TeamSpeak%203%20Server%20Query%20Manual.pdf))
- Built-in keep alive method
- Event-based system

Getting Started
===============
All functionality is contained in the [TS3Api](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/blob/master/src/com/github/theholywaffle/teamspeak3/TS3Api.java) object.

1. In order to obtain this object you first need to create a new [TS3Query](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/blob/master/src/com/github/theholywaffle/teamspeak3/TS3Query.java) object.
2. Enable debug if you wish by calling `debug()`
3. Connect to the server with `connect()`
4. Call `getApi()` to get an TS3Api object.
5. Do whatever you want with this api

Example:

    TS3Api api = new TS3Query("77.77.77.77", TS3Query.DEFAULT_PORT,FloodRate.UNLIMITED).debug().connect().getApi();
    api.login("serveradmin", "serveradminpassword");
    api.selectDefaultVirtualServer();
    api.setNickname("PutPutBot");
    ...
    
More examples can be found [here](src/com/github/theholywaffle/teamspeak3/example).
    
TODO (aka I need your help on this)
====

* Add JavaDoc to [TS3Api](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/blob/master/src/com/github/theholywaffle/teamspeak3/TS3Api.java).
* Add more methods to [TS3Api](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API/blob/master/src/com/github/theholywaffle/teamspeak3/TS3Api.java).