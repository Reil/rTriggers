package com.reil.bukkit.rTriggers;

import org.bukkit.entity.Player;

public class rTriggersTimer implements Runnable{
	String message;
	String recipients;
	rTriggers plugin;
	Player triggerer;
	
	public rTriggersTimer(rTriggers parentPlugin, String message){
		this(parentPlugin, message, null);
	}
	
	public rTriggersTimer(rTriggers parentPlugin, String message, Player triggerer){
		String[] split = message.split(":",3);
		this.recipients = split[0];
		this.message = split[2];
		this.plugin = parentPlugin;
		this.triggerer = triggerer;
	}
	@Override
	public void run() {
		String sendMe = plugin.replaceLists(message);
		
		sendMe = rTriggers.stdReplace(sendMe);

		plugin.sendMessage(sendMe, triggerer, recipients);
	}
}
