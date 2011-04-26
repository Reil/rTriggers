package com.reil.bukkit.rTriggers;

import java.util.HashSet;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.PluginManager;

import com.nijikokun.bukkit.Permissions.Permissions;

public class rTriggersServerListener extends ServerListener {
	rTriggers rTriggers;
	HashSet<String> plugins = new HashSet<String>();
	rTriggersServerListener(rTriggers rTriggers){
		this.rTriggers = rTriggers;
	}
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
	
    public void onPluginEnable(PluginEnableEvent event) {
    	String pluginName = event.getPlugin().getDescription().getName();
        if(pluginName.equals("iConomy")) {
           rTriggers.log.info("[rTriggers] Attached to iConomy.");
           rTriggers.useiConomy = true;
        }
        if(pluginName.equals("Permissions")) {
        	rTriggers.PermissionsPlugin = Permissions.Security;
            rTriggers.log.info("[rTriggers] Attached plugin to Permissions.");
        }
        if(plugins.contains(pluginName)) {
        	rTriggers.triggerMessagesWithOption(null, "onload|" + pluginName);
        }
        
    }
}
