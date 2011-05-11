package com.reil.bukkit.rTriggers;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;


public class rTriggersEntityListener extends EntityListener{
	private final rTriggers rTriggers;
	rTriggersEntityListener(rTriggers rTriggers) {
		this.rTriggers = rTriggers;
	}
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player) || event.isCancelled()) return;
		Integer gotHit = ((Player) event.getEntity()).getEntityId();
		this.rTriggers.deathCause.put(gotHit, event.getCause());
		if (event instanceof EntityDamageByEntityEvent){
			this.rTriggers.deathBringer.put(gotHit, ((EntityDamageByEntityEvent) event).getDamager());
		}
	}
	@Override
	public void onEntityDeath (EntityDeathEvent event) {
		String deathBy; 
		String triggerOption;
		if (event.getEntity() == null) return;
		if(!(event.getEntity() instanceof Player)) return;
		Player deadGuy = (Player) event.getEntity();
		Integer deadGuyId = deadGuy.getEntityId();
		EntityDamageEvent.DamageCause causeOfDeath = this.rTriggers.deathCause.get(deadGuyId);
		if (causeOfDeath == null) causeOfDeath = EntityDamageEvent.DamageCause.CUSTOM;
		switch (causeOfDeath) {
		case CONTACT:
			triggerOption = "contact";
			deathBy = "touching something";
			break;
		case ENTITY_ATTACK:
			triggerOption = "entity_attack";
			deathBy = "being hit";
			break;
		case SUFFOCATION:
			triggerOption = "suffocation";
			deathBy = "suffocation";
			break;
		case FALL:
			triggerOption = "fall";
			deathBy = "falling";
			break;
		case FIRE:
			triggerOption = "fire";
			deathBy = "fire";
			break;
		case FIRE_TICK:
			triggerOption = "fire_tick";
			deathBy = "burning";
			break;
		case LAVA:
			triggerOption = "lava";
			deathBy = "lava";
			break;
		case DROWNING:
			triggerOption = "drowning";
			deathBy = "drowning";
			break;
		case BLOCK_EXPLOSION:
			triggerOption = "block_explosion";
			deathBy = "explosion";
			break;
		case ENTITY_EXPLOSION:
			triggerOption = "entity_explosion";
			deathBy = "creeper";
			break;
		case CUSTOM:
			triggerOption = "custom";
			deathBy = "the unknown";
			break;
		default:
			triggerOption = "something";
			deathBy = "something";
			break;
		}
		if (causeOfDeath == EntityDamageEvent.DamageCause.ENTITY_ATTACK && this.rTriggers.deathBringer.get(deadGuyId) != null){
			String [] replaceThese = {"<<death-cause>>", "<<killer>>"};
			if (this.rTriggers.deathBringer.get(deadGuyId) instanceof Player) {
				String [] withThese = {deathBy, ((Player) this.rTriggers.deathBringer.get(deadGuyId)).getDisplayName() };
				rTriggers.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|playerkill", replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|entity", replaceThese, withThese);
			} else{
				String killer = this.rTriggers.deathBringer.get(deadGuyId).getClass().getName();
				killer = killer.substring(killer.lastIndexOf("Craft") + "Craft".length());
				String [] withThese = {deathBy, killer};
				rTriggers.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|" + killer, replaceThese, withThese);
				rTriggers.triggerMessages(deadGuy, "ondeath|entity", replaceThese, withThese);
			}
		} 
		else{
			String [] replaceThese = {"<<death-cause>>"};
			String [] withThese = {deathBy};
			rTriggers.triggerMessages(deadGuy, "ondeath", replaceThese, withThese);
			rTriggers.triggerMessages(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
		}
	}
}
