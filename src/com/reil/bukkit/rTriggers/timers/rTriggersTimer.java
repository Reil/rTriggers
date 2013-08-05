package com.reil.bukkit.rTriggers.timers;

import org.bukkit.entity.Player;

import com.reil.bukkit.rTriggers.Formatter;
import com.reil.bukkit.rTriggers.rTriggers;

public class rTriggersTimer implements Runnable{
	String fullmessage;
	String message;
	String options;
	String recipients;
	rTriggers plugin;
	Player triggerer;
	
	public rTriggersTimer(rTriggers parentPlugin, String message){
		this(parentPlugin, message, null);
	}
	
	public rTriggersTimer(rTriggers parentPlugin, String message, Player triggerer){
		String[] split = message.split(":",3);
		this.fullmessage=message;
		this.recipients = split[0];
		this.options    = split[1];
		this.message    = split[2];
		this.plugin = parentPlugin;
		this.triggerer = triggerer;
	}
	@Override
	public void run() {
		String sendMe = plugin.formatter.replaceCustomLists(message);
		sendMe = plugin.formatter.replaceGeneratedLists(sendMe);
		
		sendMe = Formatter.stdReplace(sendMe);
		plugin.sendMessage(sendMe, triggerer, recipients);
	}
}