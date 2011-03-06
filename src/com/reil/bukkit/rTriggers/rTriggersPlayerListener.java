package com.reil.bukkit.rTriggers;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;


public class rTriggersPlayerListener extends PlayerListener {
	/**
	 * 
	 */
	private final rTriggers rTriggers;

	/**
	 * @param rTriggers
	 */
	rTriggersPlayerListener(rTriggers rTriggers) {
		this.rTriggers = rTriggers;
	}

	public void onPlayerJoin(PlayerEvent event){
		Player triggerMessage = event.getPlayer();
		this.rTriggers.triggerMessagesWithOption(triggerMessage, "onlogin");
		return;
	}
	
	public void onPlayerQuit(PlayerEvent event){
		Player triggerMessage = event.getPlayer();
		this.rTriggers.triggerMessagesWithOption(triggerMessage, "ondisconnect");
		return;
	}
	/*
	public void onBan(Player mod, Player triggerMessage, java.lang.String reason) {
		String [] replaceThese = {"<<ban-reason>>", "<<ban-setter>>", "<<ban-recipient>>"     };
		String [] withThese =    {reason          , mod.getName()   , triggerMessage.getName()};
		this.rTriggers.triggerMessagesWithOption(triggerMessage, "onban", replaceThese, withThese);
	}*/
	public void onPlayerKick(PlayerKickEvent event){
		Player triggerMessage = event.getPlayer();
		String [] replaceThese = {"<<kick-reason>>" , "<<kickedplayer>>"     };
		String [] withThese =    {event.getReason() , triggerMessage.getName()};
		this.rTriggers.triggerMessagesWithOption(triggerMessage, "onkick", replaceThese, withThese);
	}
	
	public void onPlayerCommandPreprocess(PlayerChatEvent event){
		Player player = event.getPlayer();
		ArrayList<String> replaceThese = new ArrayList<String>();
		ArrayList<String> withThese = new ArrayList<String>();
		String [] split = event.getMessage().split(" ");
		for(int i = 1; i < split.length; i++){
			replaceThese.add("<<param" + Integer.toString(i) + ">>");
			withThese.add(split[i]);
		}
		String [] replaceTheseArray = replaceThese.toArray(new String[replaceThese.size()]);
		String [] withTheseArray = withThese.toArray(new String[withThese.size()]);

		this.rTriggers.triggerMessagesWithOption(player, "oncommand|" + split[0], replaceTheseArray, withTheseArray);
		
		
        if (split[0].equalsIgnoreCase("/rTriggers")) {
			this.rTriggers.triggerMessagesWithOption(player, "onrTriggers", replaceTheseArray, withTheseArray);
			event.setCancelled(true);
		}
		
		return; 
	}
	/*
	public boolean onConsoleCommand(String[] split) {
		if (split[0].equalsIgnoreCase("grouptell")) {
			Group iShouldExist;
        	if ((iShouldExist = etc.getDataSource().getGroup(split[1])) != null) {
	        	String tag =  "<§dServer " + Colors.White + "to §" + iShouldExist.Prefix.charAt(0) + iShouldExist.Name + Colors.White + "> ";
	        	String message = tag + etc.combineSplit(2, split, " ");
	        	this.rTriggers.sendToGroup(split[1], message);
	        	this.rTriggers.log.info("[rTriggers to " + iShouldExist.Name + "] " + etc.combineSplit(2, split, " "));
        	} else {
        		this.rTriggers.log.info("[rTriggers] Invalid group name!");
        	}
        	return true;
		}
		return false;
	}*/
}