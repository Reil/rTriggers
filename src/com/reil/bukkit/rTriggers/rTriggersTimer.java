package com.reil.bukkit.rTriggers;

import org.bukkit.entity.Player;

import com.reil.bukkit.rParser.rParser;

public class rTriggersTimer implements Runnable{
	String message;
	String recipients;
	rTriggers parentPlugin;
	Player triggerer;
	
	public rTriggersTimer(rTriggers parentPlugin, String message){
		this(parentPlugin, message, null);
	}
	
	public rTriggersTimer(rTriggers parentPlugin, String message, Player triggerer){
		String[] split = message.split(":",3);
		this.recipients = split[0];
		this.message = split[2];
		this.parentPlugin = parentPlugin;
		this.triggerer = triggerer;
	}
	@Override
	public void run() {
		String sendMe = parentPlugin.replaceLists(message);
		
		String [] replace = {"(?<!\\\\)@", "(?<!\\\\)&", "<<color>>","<<placeholder>>"};
		String [] with    = {"\n§f"      , "§"         , "§"        ,""};
		sendMe = rParser.replaceWords(sendMe, replace, with);

		parentPlugin.sendMessage(sendMe, triggerer, recipients);
	}
}
