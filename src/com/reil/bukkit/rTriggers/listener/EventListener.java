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
		plugin.triggerMessages(event.getPlayer(), "onbedenter");
		if (plugin.triggerMessages(event.getPlayer(), "onbedenter|override")) event.setCancelled(true);
	}
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event){
		this.plugin.triggerMessages(event.getPlayer(), "onbedleave");
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player triggerMessage = event.getPlayer();
		plugin.triggerMessages(triggerMessage, "onlogin");
		if (plugin.triggerMessages(triggerMessage, "onlogin|override")){
			event.setJoinMessage("");
		}
		return;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		Player triggerMessage = event.getPlayer();
		plugin.triggerMessages(triggerMessage, "ondisconnect");
		if (plugin.triggerMessages(triggerMessage, "ondisconnect|override")){
			event.setQuitMessage("");
		}
		return;
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event){
		Player triggerMessage = event.getPlayer();
		String [] replaceThese = {"<<kick-reason>>" , "<<kickedplayer>>"     };
		String [] withThese =    {event.getReason() , triggerMessage.getName()};
		plugin.triggerMessages(triggerMessage, "onkick", replaceThese, withThese);
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event){
		plugin.triggerMessages(event.getPlayer(), "onrespawn");
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		
		String [] split = event.getMessage().split(" ");
		String command = split[0].toLowerCase();
		int numParams = split.length - 1;
		if(! (plugin.optionsMap.containsKey("oncommand|" + command) ||
				plugin.optionsMap.containsKey("oncommand|" + command + "|" + numParams) ||
				plugin.optionsMap.containsKey("oncommand|" + command + "|override") ||
				plugin.optionsMap.containsKey("oncommand|" + command + "|override|" + numParams)
				)) return;
		
		List<String> replaceThese = new LinkedList<String>();
		List<String> withThese    = new LinkedList<String>();
		/* Build parameter list */
		StringBuilder params = new StringBuilder();
		StringBuilder reverseParams = new StringBuilder();
		String prefix = ""; 
		int max = split.length;
		for(int i = 1; i < max; i++){
			params.append(prefix + split[i]);
			reverseParams.insert(0, split[max - i] + prefix);
			prefix = " ";
			
			replaceThese.add("<<param" + i + ">>");
			withThese.add(split[i]);
			
			replaceThese.add("<<param" + i + "->>");
			withThese.add(params.toString());

			replaceThese.add("<<param" + (max - i) + "\\+>>");
			withThese.add(reverseParams.toString());
		}
		replaceThese.add("<<params>>");
		withThese.add(params.toString());
		String [] replaceTheseArray = replaceThese.toArray(new String[replaceThese.size()]);
		String [] withTheseArray = withThese.toArray(new String[withThese.size()]);

		plugin.triggerMessages(player, "oncommand|" + command, replaceTheseArray, withTheseArray);

        if (plugin.triggerMessages(player, "oncommand|" + command + "|override", replaceTheseArray, withTheseArray)
        		|| plugin.triggerMessages(player, "oncommand|" + command + "|override|" + numParams, replaceTheseArray, withTheseArray)){
        	event.setCancelled(true);
        }
		
		return; 
	}
	
	@EventHandler
	public void onServerCommand(ServerCommandEvent event){
		
		String [] split = event.getCommand().split(" ");
		String command = split[0].toLowerCase();
		
		int numParams = split.length - 1;
		if(! (plugin.optionsMap.containsKey("onconsole|" + command) ||
				plugin.optionsMap.containsKey("onconsole|" + command + "|" + numParams) ||
				plugin.optionsMap.containsKey("onconsole|" + command + "|override") ||
				plugin.optionsMap.containsKey("onconsole|" + command + "|override|" + numParams))) return;
		
		List<String> replaceThese = new LinkedList<String>();
		List<String> withThese    = new LinkedList<String>();
		/* Build parameter list */
		StringBuilder params = new StringBuilder();
		StringBuilder reverseParams = new StringBuilder();
		String prefix = ""; 
		int max = split.length;
		for(int i = 1; i < max; i++){
			params.append(prefix + split[i]);
			reverseParams.insert(0, split[max - i] + prefix);
			prefix = " ";
			
			replaceThese.add("<<param" + i + ">>");
			withThese.add(split[i]);
			
			replaceThese.add("<<param" + i + "->>");
			withThese.add(params.toString());

			replaceThese.add("<<param" + (max - i) + "\\+>>");
			withThese.add(reverseParams.toString());
		}
		replaceThese.add("<<params>>");
		withThese.add(params.toString());
		String [] replaceTheseArray = replaceThese.toArray(new String[replaceThese.size()]);
		String [] withTheseArray = withThese.toArray(new String[withThese.size()]);

		plugin.triggerMessages("onconsole|" + command, replaceTheseArray, withTheseArray);
		
        if (plugin.triggerMessages("onconsole|" + command + "|override", replaceTheseArray, withTheseArray)
        		|| plugin.triggerMessages("onconsole|" + command + "|override|" + numParams, replaceTheseArray, withTheseArray)){
        	event.setCommand("rTriggers");
        }
		
		return; 
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
		deathBy = rTriggers.damageCauseNatural(causeOfDeath);
		if (causeOfDeath == EntityDamageEvent.DamageCause.ENTITY_ATTACK && damageEvent instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) damageEvent).getDamager() instanceof Player){
			Player killer = (Player) ((EntityDamageByEntityEvent) damageEvent).getDamager();
			String weapon = killer.getItemInHand().getType().toString().toLowerCase().replace("_", " ");
			if (weapon.equals("air")) weapon = "fists";
			String [] replaceThese = {"<<death-cause>>", "<<killer>>"           , "<<weapon>>"};
			String [] withThese    = {deathBy          , killer.getDisplayName(), weapon };
			plugin.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			plugin.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
			plugin.triggerMessages(deadGuy, "ondeath|playerkill", replaceThese, withThese);
		} 
		else{
			String [] replaceThese = {"<<death-cause>>"};
			String [] withThese = {deathBy};
			plugin.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			plugin.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
			plugin.triggerMessages(deadGuy, "ondeath|natural", replaceThese, withThese);
			plugin.triggerMessages(deadGuy, "ondeath|natural|" + triggerOption, replaceThese, withThese);
		}
	}
}