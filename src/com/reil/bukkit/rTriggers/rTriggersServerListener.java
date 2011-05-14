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
		this.rTriggers.triggerMessages("onconsole");
	}
	public void listenFor(String pluginName) {
		plugins.add(pluginName);
	}
	
	public void checkAlreadyLoaded(PluginManager PM) {
		for(String checkMe:plugins)
			if(PM.getPlugin(checkMe) != null) rTriggers.triggerMessages("onload|" + checkMe);
	}	
	
	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        rTriggers.grabPlugins(rTriggers.pluginManager);
        
        String pluginName = event.getPlugin().getDescription().getName();
        if(plugins.contains(pluginName)) rTriggers.triggerMessages("onload|" + pluginName);
        
        if (!rTriggers.economyMethods.hasMethod() && rTriggers.economyMethods.setMethod(event.getPlugin()))
            rTriggers.economyPlugin = rTriggers.economyMethods.getMethod();
    }
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
		if (rTriggers.PermissionsPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("Permissions")) {
            	rTriggers.PermissionsPlugin = null;
                System.out.println("[rTriggers] Unattached from Permissions.");
            }
        }
        if (rTriggers.CraftIRCPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("CraftIRC")) {
            	rTriggers.CraftIRCPlugin = null;
                System.out.println("[rTriggers] Unattached from CraftIRC.");
            }
        }

        // Check to see if the plugin thats being disabled is the one we are using for economy
        if (rTriggers.economyMethods != null && rTriggers.economyMethods.hasMethod()) {
            if(rTriggers.economyMethods.checkDisabled(event.getPlugin())) {
                this.rTriggers.economyPlugin = null;
                System.out.println("[rTriggers] Payment method was disabled. No longer accepting payments.");
            }
        }
    }
}
