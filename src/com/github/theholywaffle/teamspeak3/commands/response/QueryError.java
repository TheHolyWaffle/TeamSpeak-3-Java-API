package com.github.theholywaffle.teamspeak3.commands.response;

public class QueryError extends DefaultArrayResponse {

	
	public QueryError(String raw) {
		super(raw);
	}

	public int getId(){
		return Integer.parseInt(getArray().get(0).get("id"));
	}
	
	public String getMessage(){
		return getArray().get(0).get("msg");
	}
	
	public String getExtraMessage(){
		return getArray().get(0).get("extra_msg");
	}
	
	public boolean isSuccessful(){
		return getId()==0;
	}

}
