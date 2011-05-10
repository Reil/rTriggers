package com.reil.bukkit.rTriggers;

import java.util.HashSet;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.iConomy.iConomy;
import com.nijikokun.bukkit.Permissions.Permissions;

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
    	String pluginName = event.getPlugin().getDescription().getName();
        if(pluginName.equals("Permissions")) {
        	rTriggers.PermissionsPlugin = Permissions.Security;
            rTriggers.log.info("[rTriggers] Attached to Permissions.");
        }
        if(plugins.contains(pluginName)) {
        	rTriggers.triggerMessagesWithOption(null, "onload|" + pluginName);
        }
        if(pluginName.equals("CraftIRC")) {
        	rTriggers.craftIRCHandle = (CraftIRC) event.getPlugin();
        }
        
        if (rTriggers.iConomyPlugin == null) {
            Plugin iConomyPlugin = rTriggers.getServer().getPluginManager().getPlugin("iConomy");

            if (iConomyPlugin != null) {
                if (iConomyPlugin.isEnabled()) {
                    rTriggers.iConomyPlugin = (iConomy) iConomyPlugin;
                    System.out.println("[rTriggers] Attached to iConomy.");
                }
            }
        }
    }
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
        if (rTriggers.iConomyPlugin != null) {
            if (event.getPlugin().getDescription().getName().equals("iConomy")) {
            	rTriggers.iConomyPlugin = null;
                System.out.println("[rTriggers] Unattached from iConomy.");
            }
        }
    }
}
