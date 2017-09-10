package com.reil.bukkit.rTriggers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import net.milkbowl.vault.permission.Permission;


public class PermissionsAdaptor {
	Object permPlugin;
	JavaPlugin plugin;
	PermissionType pluginType = PermissionType.NONE;
	private static Permission vaultPerms = null;
	enum PermissionType {
		NONE,
		VAULT,
		NIJIKOKUN;
	}
	
	public PermissionsAdaptor(rTriggers plugin){
		RegisteredServiceProvider<Permission> rsp =plugin. getServer().getServicesManager().getRegistration(Permission.class);
	    vaultPerms = rsp.getProvider();
	    
		if(vaultPerms == null) {
			pluginType = PermissionType.VAULT;
			plugin.log.info("[rTriggers] Attached to Vault.");
		}
		else if (plugin.getServer().getPluginManager().getPlugin("Permissions") != null){
			permPlugin = Permissions.Security;
			pluginType = PermissionType.NIJIKOKUN;
			plugin.log.info("[rTriggers] Attached to Permissions.");
		}
	}
	
	public List<String> getGroups(Player player){
		switch (pluginType) {
		case VAULT:
			return Arrays.asList(vaultPerms.getPlayerGroups(player.getWorld().getName(), player.getName()));
		case NIJIKOKUN:
			return Arrays.asList(((PermissionHandler) permPlugin).getGroups(player.getWorld().getName(),player.getName()));
		default:
			return new LinkedList<String>();
		}
	}
	
	public boolean isInGroup(Player player, String group) {
		switch (pluginType){
		case VAULT:
			return vaultPerms.playerInGroup(player.getWorld().getName(), player.getName(), group);
		case NIJIKOKUN:
			return ((PermissionHandler) permPlugin).inSingleGroup(player.getWorld().getName(), player.getName(), group);
		default:
			return false;
		}
	}
	
	public boolean hasPermission(Player player, String perm){
		switch (pluginType){
		case VAULT:
			return vaultPerms.has(player.getWorld().getName(), player.getName(), perm);
		case NIJIKOKUN:
			return ((PermissionHandler) permPlugin).has(player, perm);
		default:
			return player.hasPermission(perm);
		}
	}
}
