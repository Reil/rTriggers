package com.reil.bukkit.rTriggers;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.reil.bukkit.rParser.rParser;

public class rTriggersTimer extends TimerTask{
	String [] Messages;
	rTriggers rTriggers;
	Timer timer;
	private int progression;
	Random generator;
	private static final int random = 1;
	private static final int sequential = 0;
	int nextMessage = 0;
	int delay;
	// Listname, delay, progression
	public rTriggersTimer(rTriggers rTriggers, Timer timer, String [] Messages){
		this.Messages = Messages;
		this.rTriggers = rTriggers;
		this.timer = timer;
		this.generator = new Random();
		String [] split =  Messages[0].split(":");
		String [] options = split[1].split(",");
		this.progression = new Integer(options[2]);
		if (progression == random){
			this.nextMessage =  generator.nextInt(Messages.length);
		} else if (progression == sequential){
			this.nextMessage = 1 % Messages.length;
		} else {
			// Error?
		}
		this.delay = new Integer(options[1]) * 1000;
	}
	public rTriggersTimer() {
	}
	
	public rTriggersTimer clone(){
		rTriggersTimer clone = new rTriggersTimer();
		clone.rTriggers = this.rTriggers;
		clone.Messages = this.Messages;
		clone.nextMessage = this.nextMessage;
		clone.generator = this.generator;
		clone.timer = this.timer;
		clone.delay = this.delay;
		clone.progression = this.progression;
		return clone;
	}
	
	public void run() {
		// parse into groups, next time, 'progression'
		String toParse = Messages[nextMessage%Messages.length];
		String [] split =  toParse.split(":");
		String [] options =  split[1].split(",");
		String Groups = split[0];
		try{
			delay = new Integer(options[1]) * 1000;
		} catch (NumberFormatException blargh){
			rTriggers.log.info("[rTriggers] Invalid timer interval!");
			return;
		}
		String message = rParser.combineSplit(2, split, ":");

		// Send message
		String [] sendToGroups = Groups.split(",");
		String [] replace = {"@"	 , "<<color>>","<<placeholder>>"};
		String [] with    = {"\n§f"  , "§"        ,  ""};
		message = rParser.replaceWords(message, replace, with);
		rTriggers.sendToGroups(sendToGroups, message, null);
		
		// Find next sequence
		if (progression == random) {
			if (Messages.length != 1) {
				int prospectiveNext = nextMessage;
				while (prospectiveNext == nextMessage)
					prospectiveNext = generator.nextInt(Messages.length);
				nextMessage = prospectiveNext;
			} else nextMessage = ( nextMessage + 1 ) % Messages.length;
		}
		else if (progression == sequential)
			nextMessage = ( nextMessage + 1 ) % Messages.length;
		else {
			// TODO: I am error!
		}
		
		// Shit bricks
		// Schedule next run
		rTriggersTimer clone = (rTriggersTimer) this.clone();
		timer.schedule(clone, delay);
	}

}
