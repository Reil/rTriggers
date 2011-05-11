package com.reil.bukkit.rTriggers;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.player.*;


public class rTriggersPlayerListener extends PlayerListener {
	private final rTriggers rTriggers;

	/**
	 * @param rTriggers
	 */
	rTriggersPlayerListener(rTriggers rTriggers) {
		this.rTriggers = rTriggers;
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent event){
		Player triggerMessage = event.getPlayer();
		this.rTriggers.triggerMessages(triggerMessage, "onlogin");
		if (this.rTriggers.triggerMessages(triggerMessage, "onlogin|override")){
			event.setJoinMessage("");
		}
		return;
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent event){
		Player triggerMessage = event.getPlayer();
		this.rTriggers.triggerMessages(triggerMessage, "ondisconnect");
		if (this.rTriggers.triggerMessages(triggerMessage, "ondisconnect|override")){
			event.setQuitMessage("");
		}
		this.rTriggers.deathCause.remove(triggerMessage.getEntityId());
		this.rTriggers.deathBringer.remove(triggerMessage.getEntityId());
		return;
	}
	
	@Override
	public void onPlayerKick(PlayerKickEvent event){
		Player triggerMessage = event.getPlayer();
		String [] replaceThese = {"<<kick-reason>>" , "<<kickedplayer>>"     };
		String [] withThese =    {event.getReason() , triggerMessage.getName()};
		this.rTriggers.triggerMessages(triggerMessage, "onkick", replaceThese, withThese);
	}
	
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){
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

		this.rTriggers.triggerMessages(player, "oncommand|" + split[0], replaceTheseArray, withTheseArray);
		
		
        if (split[0].equalsIgnoreCase("/rTriggers")) {
			this.rTriggers.triggerMessages(player, "onrTriggers", replaceTheseArray, withTheseArray);
			event.setCancelled(true);
		}
        
        if (this.rTriggers.triggerMessages(player, "oncommand|" + split[0] + "|override", replaceTheseArray, withTheseArray)){
        	event.setCancelled(true);
        }
		
		return; 
	}
}