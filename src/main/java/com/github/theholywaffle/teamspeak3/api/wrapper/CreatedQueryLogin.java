package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Map;

public class CreatedQueryLogin extends QueryLogin {

    public CreatedQueryLogin(Map<String, String> map) {
        super(map);
    }

    public String getQueryClientLoginPassword() {
        return require("client_login_password");
    }
}
