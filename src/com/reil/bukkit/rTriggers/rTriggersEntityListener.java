package com.reil.bukkit.rTriggers;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;


public class rTriggersEntityListener extends EntityListener{
	private final rTriggers rTriggersPlugin;
	rTriggersEntityListener(rTriggers rTriggers) {
		this.rTriggersPlugin = rTriggers;
	}
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player) || event.isCancelled()) return;
		Integer gotHit = ((Player) event.getEntity()).getEntityId();
		this.rTriggersPlugin.deathCause.put(gotHit, event.getCause());
		if (event instanceof EntityDamageByEntityEvent){
			this.rTriggersPlugin.deathBringer.put(gotHit, ((EntityDamageByEntityEvent) event).getDamager());
		}
	}
	@Override
	public void onEntityDeath (EntityDeathEvent event) {
		String deathBy; 
		String triggerOption;
		if (event.getEntity() == null || !(event.getEntity() instanceof Player)) return;
		Player deadGuy = (Player) event.getEntity();
		Integer deadGuyId = deadGuy.getEntityId();
		EntityDamageEvent.DamageCause causeOfDeath = this.rTriggersPlugin.deathCause.get(deadGuyId);
		if (causeOfDeath == null) causeOfDeath = EntityDamageEvent.DamageCause.CUSTOM;
		triggerOption = causeOfDeath.toString().toLowerCase();
		deathBy = rTriggers.damageCauseNatural(causeOfDeath);
		if (causeOfDeath == EntityDamageEvent.DamageCause.ENTITY_ATTACK && this.rTriggersPlugin.deathBringer.get(deadGuyId) != null){
			String [] replaceThese = {"<<death-cause>>", "<<killer>>"};
			if (this.rTriggersPlugin.deathBringer.get(deadGuyId) instanceof Player) {
				String [] withThese = {deathBy, ((Player) this.rTriggersPlugin.deathBringer.get(deadGuyId)).getDisplayName() };
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|playerkill", replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|entity", replaceThese, withThese);
			} else{
				String killer = this.rTriggersPlugin.deathBringer.get(deadGuyId).getClass().getName();
				killer = killer.substring(killer.lastIndexOf("Craft") + "Craft".length());
				String [] withThese = {deathBy, killer};
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|" + killer, replaceThese, withThese);
				rTriggersPlugin.triggerMessages(deadGuy, "ondeath|entity", replaceThese, withThese);
			}
		} 
		else{
			String [] replaceThese = {"<<death-cause>>"};
			String [] withThese = {deathBy};
			rTriggersPlugin.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			rTriggersPlugin.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
		}
	}
}
