package com.reil.bukkit.rTriggers;

import java.util.HashSet;

import org.bukkit.event.server.*;
import org.bukkit.plugin.*;

import com.nijikokun.register.payment.Methods;

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
        
        if (plugin.useRegister && !Methods.hasMethod()) Methods.setMethod(rTriggers.MCServer.getPluginManager());
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
        if (plugin.useRegister && Methods.hasMethod() && Methods.checkDisabled(event.getPlugin())) {
            System.out.println("[rTriggers] Payment method was disabled. No longer accepting payments.");
        }
    }
}
