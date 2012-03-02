package com.reil.bukkit.rTriggers.timers;

import java.util.Calendar;

import org.bukkit.scheduler.BukkitScheduler;

import com.reil.bukkit.rTriggers.rTriggers;

public class TimeKeeper implements Runnable {
	long delay;
	long GMTOffset;
	rTriggers plugin;
	public TimeKeeper(rTriggers plugin, BukkitScheduler bukkitScheduler, int timeZone){
		// find # of system ticks until next minute.
		delay = (60000 - (System.currentTimeMillis() % 60000)) / 50;
		GMTOffset = 3600000 * timeZone;
		bukkitScheduler.scheduleSyncRepeatingTask(plugin, this, delay, 1200);
		this.plugin = plugin;
	}
	
	public String replaceTimeTags (String message){
		return message;
	}
	
	@Override
	public void run() {
		int minute = Calendar.getInstance().get(Calendar.MINUTE);
		String [] messages = plugin.Messages.getStrings("<<t|" + minute + ">>");
		if (messages == null) return;
		
		for (String message : messages){
			String[] split = message.split(":",3);
			String recipients = split[0];
			String sendMe = plugin.replaceLists(split[2]);
			
			sendMe = rTriggers.stdReplace(sendMe);
			
			plugin.sendMessage(sendMe, null, recipients);
		}
	}

}