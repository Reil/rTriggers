package com.reil.bukkit.rTriggers.persistence;

import java.util.List;

import org.bukkit.entity.Player;

import com.avaje.ebean.EbeanServer;
import com.reil.bukkit.rTriggers.rTriggers;

public class LimitTracker {
	rTriggers plugin;
	EbeanServer database;
	public LimitTracker (rTriggers rTriggersPlugin) {
		plugin = rTriggersPlugin;
		database = rTriggersPlugin.getDatabase();
	}
	 /*
	 * Checks to see if:
	 * 1) This message has a limited rate of execution.
	 * 2) An appropriate amount of time has passed if it does.
	 */
	public boolean tooSoon(String message, Player triggerer) {
		String triggerName;
		if (plugin.optionsMap.containsKey("limit") && plugin.optionsMap.get("limit").contains(message)){
			triggerName = "&rTriggers";
		} else if (triggerer != null &&
				plugin.optionsMap.containsKey("limit|pertriggerer")&& 
				plugin.optionsMap.get("limit|pertriggerer").contains(message)) {
			triggerName = triggerer.getName();
		} else return false;
		
		long currentTime = System.currentTimeMillis();
		
		TriggerLimit limit = database.find(TriggerLimit.class).where().ieq("playerName", triggerName).ieq("message", message).findUnique();
		if (limit == null){
			limit = new TriggerLimit();
			limit.setPlayerName(triggerName);
			limit.setMessage(message);
			limit.setTime(currentTime);
			database.save(limit);
			return false;
		}
		
		long lastTime = limit.getTime();
		// Find minimum wait time
		long delay = 0;
		for(String checkOption : message.split(rTriggers.colonSplit)[1].split(rTriggers.commaSplit)) {
			if (checkOption.startsWith("limit|")) {
				delay = 1000 * new Long(checkOption.substring(6));
				break;
			}
		}			
		if (currentTime - lastTime < delay) return true;
		database.save(limit);
		return false;
	}
	
	public int cleanEntriesOlderThan(int milliseconds){
		long beforeThis = System.currentTimeMillis() - milliseconds;
		List<TriggerLimit> old = database.find(TriggerLimit.class).where().lt("time", beforeThis).findList();
		database.delete(old);
		return 0;
	}
}
