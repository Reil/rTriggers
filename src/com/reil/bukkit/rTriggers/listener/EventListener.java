package com.reil.bukkit.rTriggers.listener;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;

import com.reil.bukkit.rTriggers.Formatter;
import com.reil.bukkit.rTriggers.rTriggers;


public class EventListener implements Listener {
	private final rTriggers plugin;

	/**
	 * @param rTriggers
	 */
	public EventListener(rTriggers rTriggers) {
		plugin = rTriggers;
	}
	
	@EventHandler
	public void onPlayerBedEnter(PlayerBedEnterEvent event){
		plugin.dispatcher.triggerMessages(event.getPlayer(), "onbedenter");
		if (plugin.dispatcher.triggerMessages(event.getPlayer(), "onbedenter|override")) event.setCancelled(true);
	}
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event){
		this.plugin.dispatcher.triggerMessages(event.getPlayer(), "onbedleave");
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player triggerMessage = event.getPlayer();
		plugin.dispatcher.triggerMessages(triggerMessage, "onlogin");
		if (plugin.dispatcher.triggerMessages(triggerMessage, "onlogin|override")){
			event.setJoinMessage("");
		}
		return;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		Player triggerMessage = event.getPlayer();
		plugin.dispatcher.triggerMessages(triggerMessage, "ondisconnect");
		if (plugin.dispatcher.triggerMessages(triggerMessage, "ondisconnect|override")){
			event.setQuitMessage("");
		}
		return;
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event){
		Player triggerMessage = event.getPlayer();
		String [] replaceThese = {"<<kick-reason>>" , "<<kickedplayer>>"     };
		String [] withThese =    {event.getReason() , triggerMessage.getName()};
		plugin.dispatcher.triggerMessages(triggerMessage, "onkick", replaceThese, withThese);
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event){
		plugin.dispatcher.triggerMessages(event.getPlayer(), "onrespawn");
	}

	
	@EventHandler
	public void onEntityDeath (EntityDeathEvent event) {
		String deathBy; 
		String triggerOption;
		if (event.getEntity() == null || !(event.getEntity() instanceof Player)) return;
		
		EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
		EntityDamageEvent.DamageCause causeOfDeath = null;
		Player deadGuy = (Player) event.getEntity();
		
		if (damageEvent != null)  causeOfDeath = damageEvent.getCause();
		if (causeOfDeath == null) causeOfDeath = EntityDamageEvent.DamageCause.CUSTOM;
		triggerOption = causeOfDeath.toString().toLowerCase();
		deathBy = Formatter.damageCauseNatural(causeOfDeath);
		if (causeOfDeath == EntityDamageEvent.DamageCause.ENTITY_ATTACK && damageEvent instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) damageEvent).getDamager() instanceof Player){
			Player killer = (Player) ((EntityDamageByEntityEvent) damageEvent).getDamager();
			String weapon = killer.getItemInHand().getType().toString().toLowerCase().replace("_", " ");
			if (weapon.equals("air")) weapon = "fists";
			String [] replaceThese = {"<<death-cause>>", "<<killer>>"           , "<<weapon>>"};
			String [] withThese    = {deathBy          , killer.getDisplayName(), weapon };
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath|playerkill", replaceThese, withThese);
		} 
		else{
			String [] replaceThese = {"<<death-cause>>"};
			String [] withThese = {deathBy};
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath|natural", replaceThese, withThese);
			plugin.dispatcher.triggerMessages(deadGuy, "ondeath|natural|" + triggerOption, replaceThese, withThese);
		}
	}
}