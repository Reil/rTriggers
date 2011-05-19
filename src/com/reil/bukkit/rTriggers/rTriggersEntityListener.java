package com.reil.bukkit.rTriggers;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;


public class rTriggersEntityListener extends EntityListener{
	private final rTriggers plugin;
	rTriggersEntityListener(rTriggers rTriggers) {
		this.plugin = rTriggers;
	}
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player) || event.isCancelled()) return;
		Integer gotHit = ((Player) event.getEntity()).getEntityId();
		plugin.deathCause.put(gotHit, event.getCause());
		if (event instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player){
			plugin.deathBringer.put(gotHit, (Player) ((EntityDamageByEntityEvent) event).getDamager());
		}
	}
	@Override
	public void onEntityDeath (EntityDeathEvent event) {
		String deathBy; 
		String triggerOption;
		if (event.getEntity() == null || !(event.getEntity() instanceof Player)) return;
		Player deadGuy = (Player) event.getEntity();
		Integer deadGuyId = deadGuy.getEntityId();
		EntityDamageEvent.DamageCause causeOfDeath = plugin.deathCause.get(deadGuyId);
		if (causeOfDeath == null) causeOfDeath = EntityDamageEvent.DamageCause.CUSTOM;
		triggerOption = causeOfDeath.toString().toLowerCase();
		deathBy = rTriggers.damageCauseNatural(causeOfDeath);
		if (causeOfDeath == EntityDamageEvent.DamageCause.ENTITY_ATTACK && plugin.deathBringer.get(deadGuyId) != null){
			Player killer = plugin.deathBringer.get(deadGuyId);
			String weapon = killer.getItemInHand().getType().toString().toLowerCase().replace("_", " ");
			if (weapon == "air") weapon = "fists";
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
