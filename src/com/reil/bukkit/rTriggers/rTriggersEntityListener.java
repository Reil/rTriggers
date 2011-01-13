package com.reil.bukkit.rTriggers;
import org.bukkit.event.entity.*;


public class rTriggersEntityListener extends EntityListener{
	private final rTriggers rMotD;
	rTriggersEntityListener(rTriggers rMotD) {
		this.rMotD = rMotD;
	}
	public void onEntityDamagedByBlock (EntityDamagedByBlockEvent event){
		triggerDamage(event);
	}
	public void onEntityDamagedByEntity (EntityDamagedByEntityEvent event){
		triggerDamage(event);
	}
	public void triggerDamage(EntityDamagedEvent event) {
		String deathBy; 
		switch (event.getCause()) {
		case DROWNING:
			deathBy = "drowning";
			break;
		default:
			deathBy = "something";
			break;
		}
	}
}
