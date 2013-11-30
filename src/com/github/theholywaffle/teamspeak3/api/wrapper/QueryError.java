package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.HashMap;

public class QueryError extends Wrapper {

	public QueryError(HashMap<String, String> map) {
		super(map);
	}

	public int getId() {
		return getInt("id");
	}

	public String getMessage() {
		return get("msg");
	}

	public String getExtraMessage() {
		return get("extra_msg");
	}

	public int getFailedPermissionId() {
		return getInt("failed_permid");
	}

	public boolean isSuccessful() {
		return getId() == 0;
	}

}
