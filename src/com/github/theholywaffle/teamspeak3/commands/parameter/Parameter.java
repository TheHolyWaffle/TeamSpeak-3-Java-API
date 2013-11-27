package com.github.theholywaffle.teamspeak3.commands.parameter;

public abstract class Parameter {
	
	public abstract String build();
	
	public String toString(){
		return build();
	}

}
