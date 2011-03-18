package com.reil.bukkit.rTriggers;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;

public class rTriggersServerListener extends ServerListener {
	rTriggers rTriggers;
	rTriggersServerListener(rTriggers rTriggers){
		this.rTriggers = rTriggers;
	}
	public void onServerCommand(ServerCommandEvent event){
		this.rTriggers.triggerMessagesWithOption(null, "onconsole");
	}
}
