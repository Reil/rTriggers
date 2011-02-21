package com.reil.bukkit.rTriggers;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;


public class rTriggersEntityListener extends EntityListener{
	private final rTriggers rTriggers;
	HashMap <Player, EntityDamageEvent.DamageCause> deathCause = new HashMap <Player, EntityDamageEvent.DamageCause>();
	rTriggersEntityListener(rTriggers rTriggers) {
		this.rTriggers = rTriggers;
	}
	
	
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player) || event.isCancelled()) return;
		deathCause.put((Player) event.getEntity(), event.getCause());
	}
	public void onEntityDeath (EntityDeathEvent event) {
		String deathBy; 
		String triggerOption;
		if(!(event.getEntity() instanceof Player)) return;
		Player deadGuy = (Player) event.getEntity();
		EntityDamageEvent.DamageCause causeOfDeath = deathCause.get(deadGuy);
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
		String [] replaceThese = {"<<death-cause>>"};
		String [] withThese = {deathBy};
		rTriggers.triggerMessagesWithOption(deadGuy, "ondeath", replaceThese, withThese);
		rTriggers.triggerMessagesWithOption(deadGuy, "ondeath|" + triggerOption, replaceThese, withThese);
	}
}
