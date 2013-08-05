package com.reil.bukkit.rTriggers.listener;

import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.*;
import org.bukkit.plugin.*;

import com.nijikokun.register.payment.Methods;
import com.reil.bukkit.rTriggers.rTriggers;

public class SetupListener implements Listener {
	rTriggers plugin;
	HashSet<String> watchPlugins = new HashSet<String>();
	
	public SetupListener(rTriggers rTriggers){
		this.plugin = rTriggers;
	}

	public void listenFor(String pluginName) {
		watchPlugins.add(pluginName);
	}
	
	public void checkAlreadyLoaded(PluginManager PM) {
		for(String checkMe:watchPlugins)
			if(PM.getPlugin(checkMe) != null) plugin.dispatcher.dispatchEvents("onload|" + checkMe);
	}	
	
	@EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        plugin.grabPlugins(plugin.getServer().getPluginManager());
        
        String pluginName = event.getPlugin().getDescription().getName();
        if(watchPlugins.contains(pluginName)) plugin.dispatcher.dispatchEvents("onload|" + pluginName);
        
        if (plugin.useRegister && !Methods.hasMethod()) Methods.setMethod(plugin.getServer().getPluginManager());
    }
	
	@EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (plugin.CraftIRCPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("CraftIRC")) {
            	plugin.CraftIRCPlugin = null;
                System.out.println("[rTriggers] Unattached from CraftIRC.");
            }
        }

        // Check to see if the plugin thats being disabled is the one we are using for economy
        if (plugin.useRegister && Methods.hasMethod() && Methods.checkDisabled(event.getPlugin())) {
            System.out.println("[rTriggers] Payment method was disabled. No longer accepting payments.");
        }
    }
}
