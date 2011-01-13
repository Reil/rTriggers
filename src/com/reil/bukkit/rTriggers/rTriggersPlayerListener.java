package com.reil.bukkit.rTriggers;
import org.bukkit.Color;
import org.bukkit.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;


@SuppressWarnings("unused")
public class rTriggersPlayerListener extends PlayerListener {
	/**
	 * 
	 */
	private final rTriggers rMotD;

	/**
	 * @param rMotD
	 */
	rTriggersPlayerListener(rTriggers rMotD) {
		this.rMotD = rMotD;
	}

	public void onPlayerJoin(PlayerEvent event){
		Player triggerMessage = event.getPlayer();
		this.rMotD.triggerMessagesWithOption(triggerMessage, "onlogin");
		return;
	}
	
	public void onPlayerQuit(PlayerEvent event){
		Player triggerMessage = event.getPlayer();
		this.rMotD.triggerMessagesWithOption(triggerMessage, "ondisconnect");
		return;
	}
	/*
	public boolean onHealthChange(Player triggerMessage, int oldValue, int newValue){
		if (newValue <= 0) {
			this.rMotD.triggerMessagesWithOption(triggerMessage, "ondeath");
		}
		return false;
	}
	
	public void onBan(Player mod, Player triggerMessage, java.lang.String reason) {
		String [] replaceThese = {"<<ban-reason>>", "<<ban-setter>>", "<<ban-recipient>>"     };
		String [] withThese =    {reason          , mod.getName()   , triggerMessage.getName()};
		this.rMotD.triggerMessagesWithOption(triggerMessage, "onban", replaceThese, withThese);
	}*/
	
	public void onPlayerCommand(PlayerChatEvent event){
		Player player = event.getPlayer();
		String [] split = event.getMessage().split(" ");
		
		this.rMotD.triggerMessagesWithOption(player, "oncommand:" + split[0]);
		this.rMotD.triggerMessagesWithOption(player, "oncommand|" + split[0]);
		/*
        if (split[0].equalsIgnoreCase("/grouptell")){
        	Group iShouldExist;
        	if ((iShouldExist = etc.getDataSource().getGroup(split[1])) != null) {
	        	String tag =  "<" + player.getColor() + player.getName() + Color.WHITE + " to §" + iShouldExist.Prefix.charAt(0) + iShouldExist.Name + Color.WHITE + "> ";
	        	String message = tag + MessageParser.combineSplit(2, split, " ");
	        	String [] functionParam = {split[1], player.getName()};
	        	this.rMotD.sendToGroups(functionParam, message,player);
        	} else {
        		player.sendMessage(Color.RED + "Invalid group name!");
        	}
        	event.setCancelled(true);
        	return;
        }    */
        if (split[0].equalsIgnoreCase("/rmotd")) {
			this.rMotD.triggerMessagesWithOption(player, "onrmotd");
			event.setCancelled(true);
		}
		
		return; 
	}
	/*
	public boolean onConsoleCommand(String[] split) {
		if (split[0].equalsIgnoreCase("grouptell")) {
			Group iShouldExist;
        	if ((iShouldExist = etc.getDataSource().getGroup(split[1])) != null) {
	        	String tag =  "<§dServer " + Colors.White + "to §" + iShouldExist.Prefix.charAt(0) + iShouldExist.Name + Colors.White + "> ";
	        	String message = tag + etc.combineSplit(2, split, " ");
	        	this.rMotD.sendToGroup(split[1], message);
	        	this.rMotD.log.info("[rMotd to " + iShouldExist.Name + "] " + etc.combineSplit(2, split, " "));
        	} else {
        		this.rMotD.log.info("[rMotD] Invalid group name!");
        	}
        	return true;
		}
		return false;
	}*/
}