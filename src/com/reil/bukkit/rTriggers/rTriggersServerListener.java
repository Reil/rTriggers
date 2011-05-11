package com.reil.bukkit.rTriggers;

import java.util.HashSet;

import org.bukkit.event.server.*;
import org.bukkit.plugin.*;

public class rTriggersServerListener extends ServerListener {
	rTriggers rTriggers;
	HashSet<String> plugins = new HashSet<String>();
	rTriggersServerListener(rTriggers rTriggers){
		this.rTriggers = rTriggers;
	}
	@Override
	public void onServerCommand(ServerCommandEvent event){
		this.rTriggers.triggerMessagesWithOption(null, "onconsole");
	}
	public void listenFor(String pluginName) {
		plugins.add(pluginName);
	}
	
	public void checkAlreadyLoaded() {
		PluginManager PM = rTriggers.MCServer.getPluginManager();
		for(String checkMe:plugins){
			if(PM.getPlugin(checkMe) != null){
				rTriggers.triggerMessagesWithOption(null, "onload|" + checkMe);
			}
		}
	}	
	
	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        rTriggers.grabPlugins();
        
        String pluginName = event.getPlugin().getDescription().getName();
        if(plugins.contains(pluginName)) {
        	rTriggers.triggerMessagesWithOption(null, "onload|" + pluginName);
        }
    }
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
		if (rTriggers.PermissionsPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("Permissions")) {
            	rTriggers.PermissionsPlugin = null;
                System.out.println("[rTriggers] Unattached from Permissions.");
            }
        }
        if (rTriggers.iConomyPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("iConomy")) {
            	rTriggers.iConomyPlugin = null;
                System.out.println("[rTriggers] Unattached from iConomy.");
            }
        }
        if (rTriggers.CraftIRCPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("CraftIRC")) {
            	rTriggers.CraftIRCPlugin = null;
                System.out.println("[rTriggers] Unattached from CraftIRC.");
            }
        }
    }
}
