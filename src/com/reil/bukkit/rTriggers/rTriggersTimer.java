package com.reil.bukkit.rTriggers;

import com.reil.bukkit.rParser.rParser;

public class rTriggersTimer implements Runnable{
	String message;
	String recipients;
	rTriggers parentPlugin;
	
	public rTriggersTimer(rTriggers parentPlugin, String message){
		this.recipients = message.split(":")[0];
		this.message = rParser.combineSplit(2, message.split(":"), ":");
		this.parentPlugin = parentPlugin;
	}
	@Override
	public void run() {
		String sendMe = parentPlugin.replaceLists(message);
		
		String [] replace = {"(?<!\\\\)@", "(?<!\\\\)&", "<<color>>","<<placeholder>>"};
		String [] with    = {"\n§f"      , "§"         , "§"        ,""};
		sendMe = rParser.replaceWords(sendMe, replace, with);

		parentPlugin.sendMessage(sendMe, null, recipients);
	}
}
