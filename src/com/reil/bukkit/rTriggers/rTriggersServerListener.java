package com.reil.bukkit.rTriggers;

import java.util.HashSet;

import org.bukkit.event.server.*;
import org.bukkit.plugin.*;

public class rTriggersServerListener extends ServerListener {
	rTriggers plugin;
	HashSet<String> watchPlugins = new HashSet<String>();
	
	rTriggersServerListener(rTriggers rTriggers){
		this.plugin = rTriggers;
	}
	@Override
	public void onServerCommand(ServerCommandEvent event){
		plugin.triggerMessages("onconsole");
	}
	public void listenFor(String pluginName) {
		watchPlugins.add(pluginName);
	}
	
	public void checkAlreadyLoaded(PluginManager PM) {
		for(String checkMe:watchPlugins)
			if(PM.getPlugin(checkMe) != null) plugin.triggerMessages("onload|" + checkMe);
	}	
	
	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        plugin.grabPlugins(plugin.pluginManager);
        
        String pluginName = event.getPlugin().getDescription().getName();
        if(watchPlugins.contains(pluginName)) plugin.triggerMessages("onload|" + pluginName);
        
        if (!plugin.economyMethods.hasMethod() && plugin.economyMethods.setMethod(event.getPlugin()))
            plugin.economyPlugin = plugin.economyMethods.getMethod();
    }
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
		if (plugin.PermissionsPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("Permissions")) {
            	plugin.PermissionsPlugin = null;
                System.out.println("[rTriggers] Unattached from Permissions.");
            }
        }
        if (plugin.CraftIRCPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("CraftIRC")) {
            	plugin.CraftIRCPlugin = null;
                System.out.println("[rTriggers] Unattached from CraftIRC.");
            }
        }

        // Check to see if the plugin thats being disabled is the one we are using for economy
        if (plugin.economyMethods != null && plugin.economyMethods.hasMethod()) {
            if(plugin.economyMethods.checkDisabled(event.getPlugin())) {
                this.plugin.economyPlugin = null;
                System.out.println("[rTriggers] Payment method was disabled. No longer accepting payments.");
            }
        }
    }
}
