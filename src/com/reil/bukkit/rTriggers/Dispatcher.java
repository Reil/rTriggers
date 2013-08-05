package com.reil.bukkit.rTriggers;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.croemmich.serverevents.ServerEvents;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerCommandEvent;

import com.reil.bukkit.rParser.rParser;
import com.reil.bukkit.rTriggers.persistence.LimitTracker;
import com.reil.bukkit.rTriggers.timers.rTriggersTimer;

public class Dispatcher {
	public static final String commaSplit = "[ \t]*,[ \t]*";
	public static final String colonSplit = "[ \t]*:[ \t]*";
	
	public LimitTracker limitTracker;
	List<String> permissionTriggerers;
		
	public Map <String, HashSet<String>> optionsMap;
	
	public Dispatcher() {
		limitTracker = new LimitTracker();
		optionsMap = new HashMap<String, HashSet<String>>();
		permissionTriggerers = new LinkedList<String>();
	}
   /**
    * Looks through all of the messages,
	* Sends the messages triggered by groups which 'triggerMessage' is a member of,
	* But only if that message has the contents of 'option' as one of its options
	*/
	public boolean triggerMessages(String option){ return triggerMessages(null, option); }
	public boolean triggerMessages(Player triggerMessage, String option){ return triggerMessages(triggerMessage, option, new String[0], new String[0]);	}
	public boolean triggerMessages(String option, String[] eventToReplace, String []eventReplaceWith){ return triggerMessages(null, option, eventToReplace, eventReplaceWith);}
	
	public boolean triggerMessages(Player triggerer, String option, String[] eventToReplace, String[] eventReplaceWith){
		/* Send all message candidates */
		Set<String> sendThese = getMessages(triggerer, option);
		
		for (String fullMessage : sendThese){	
			if (limitTracker.tooSoon(fullMessage, triggerer)) continue; // Don't send messages if they have the limit option and it's been too soon.
			
			String [] split =  fullMessage.split(colonSplit, 3);
			/**************************
			 * Tag replacement start!
			 *************************/
			String message = split[2];
			
			if (eventToReplace.length > 0) {
				message = rParser.replaceWords(message, eventToReplace, eventReplaceWith);
				split[0] = rParser.replaceWords(split[0], eventToReplace, eventReplaceWith);
				split[1] = rParser.replaceWords(split[1], eventToReplace, eventReplaceWith);
			}
			
			message = rTriggers.plugin.formatter.replaceCustomLists(split[2]);
			message = rTriggers.plugin.formatter.replaceGeneratedLists(message);
			
			// Regex's which catch @, but not \@ and &, but not \&
			
			message = Formatter.stdReplace(message);
			
			final String [] replace = { "<<triggerer>>", "<<triggerer-displayname>>", "<<triggerer-ip>>", "<<triggerer-locale>>", "<<triggerer-country>>", "<<triggerer-balance>>", };
			message = rParser.replaceWords(message, replace, rTriggers.plugin.formatter.getTagReplacements(triggerer));
			
			if (eventToReplace.length > 0) {
				message = rParser.replaceWords(message, eventToReplace, eventReplaceWith);
				split[0] = rParser.replaceWords(split[0], eventToReplace, eventReplaceWith);
				split[1] = rParser.replaceWords(split[1], eventToReplace, eventReplaceWith);
			}
			
			/**************************
			 *  Tag replacement end! */
			
			sendMessageCheckDelay(triggerer, fullMessage, message);
		}
		return !sendThese.isEmpty();
	}
	
	// Message has had the 
	public void sendMessageCheckDelay(Player triggerer, String fullMessage, String message) {
		// Ship out the message.  If it has a delay on it, put it on the scheduler
		String[] split = fullMessage.split(colonSplit, 3);
		if (!optionsMap.containsKey("delay") || !optionsMap.get("delay").contains(fullMessage)) {
			sendMessage(message, triggerer, split[0]); 
		} else {
			long waitTime = 0;
			for(String checkOption : split[1].split(commaSplit)) {
				if (checkOption.startsWith("delay|")) {
					try{
						waitTime = 20 * Long.parseLong(checkOption.substring(6));
					} catch (NumberFormatException e) {
						rTriggers.plugin.log.info("[rTriggers] Bad number format on option: " + checkOption + "\n in message: " + fullMessage);
						continue;
					}
					// Note, this doesn't actually -remove- the entire delay option, it just reduces it to a number, which the option parser should ignore.
					fullMessage = split[0] + ":" + split[1].replaceAll("delay|","") + ":" + message;
					rTriggers.plugin.getServer().getScheduler().scheduleSyncDelayedTask (rTriggers.plugin,
							new rTriggersTimer(fullMessage, triggerer),
							waitTime);
				}
			}
		}
	}
	
	public Set<String> getMessages(Player triggerer, String option) {
		if (!optionsMap.containsKey(option)) return new HashSet<String>(); 		// This option does not trigger anything
		
		/* Build list of groups */
		List<String> groupArray = new LinkedList<String>();
		if (triggerer != null){
			/* Everyone has at least these two. */
			groupArray.add("<<player|" + triggerer.getName() + ">>");
			groupArray.add("<<everyone>>");
			/* Add any groups the user's a member of. */
			groupArray.addAll(rTriggers.plugin.permAdaptor.getGroups(triggerer));
			if(triggerer.isOp()){
				groupArray.add("<<ops>>");
				groupArray.add("<<op>>");
			}
		} else groupArray.add("<<customtrigger>>");
		
		/* Build set of message candidates */
		Set<String> sendThese = new LinkedHashSet<String>();
		for (String groupName : groupArray)
			if(rTriggers.plugin.Messages.keyExists(groupName)) sendThese.addAll(Arrays.asList(rTriggers.plugin.Messages.getStrings(groupName)));
		if (triggerer != null){
			for(String permission : permissionTriggerers){
				String permString = "<<hasperm|" + permission + ">>";
				if(rTriggers.plugin.permAdaptor.hasPermission(triggerer,permission)) {
					if (rTriggers.plugin.Messages.keyExists(permString)) sendThese.addAll(Arrays.asList(rTriggers.plugin.Messages.getStrings(permString)));
				} else {
					if (rTriggers.plugin.Messages.keyExists("not|" + permString)) sendThese.addAll(Arrays.asList(rTriggers.plugin.Messages.getStrings("not|" + permString)));
				}
			}
		}
		
		// Remove candidates that aren't for this option
		sendThese.retainAll(optionsMap.get(option));
		return sendThese;
	}

	public void sendMessage(String message, Player triggerer, String groups){
		/* Default: Send to player unless other groups are specified.
		 * If so, send to those instead. */
		if (groups.isEmpty() || groups.equalsIgnoreCase("<<triggerer>>")) {
			sendToPlayer(message, triggerer, false, false);
			return;
		}
		
		final String [] replace = {"<<recipient>>", "<<recipient-displayname>>", "<<recipient-ip>>", "<<recipient-color>>", "<<recipient-balance>>", "§"};
		
		Set <String> sendToGroupsFiltered     = new HashSet <String>();
		Set <String> dontSendToGroupsFiltered = new HashSet <String>();
		
		Set <String> sendToPermissions     = new HashSet <String>();
		Set <String> dontSendToPermissions = new HashSet <String>();
		
		Set <Player> sendToUs     = new HashSet<Player>();
		Set <Player> dontSendToUs = new HashSet<Player>();
		dontSendToUs.add(null);
		
		World onlyHere = null;
		Server MCServer = rTriggers.plugin.getServer();
		
		boolean flagCommand  = false;
		boolean flagSay      = false;
		/*************************************
		 * Begin:
		 * 1) Constructing list of groups to send to
		 * 2) Processing 'special' groups (ones in double-chevrons) */
		for (String group : groups.split(commaSplit)){
			if (group.startsWith("not|")){
				String notTarget = group.substring(4);
				if (!notTarget.startsWith("<<")) dontSendToGroupsFiltered.add(notTarget);
				else if (notTarget.equalsIgnoreCase("<<triggerer>>")) dontSendToUs.add(triggerer);
				else if (notTarget.startsWith("<<player|")){
					String playerName = notTarget.substring(9, notTarget.length()-2);
					Player putMe = MCServer.getPlayer(playerName);
					if (putMe != null) dontSendToUs.add(putMe);
				}
				else if(notTarget.startsWith("<<hasperm|")) dontSendToPermissions.add(notTarget.substring(10, notTarget.length() - 2));
				else if(notTarget.startsWith("<<inworld|")) dontSendToUs.addAll(MCServer.getWorld(notTarget.substring(10, group.length() - 2)).getPlayers());
			}
			else if (!group.startsWith("<<")) sendToGroupsFiltered.add(group);
			/* Special cases: start! */
			else if (group.equalsIgnoreCase("<<everyone>>"))          for (Player addMe : MCServer.getOnlinePlayers()) sendToUs.add(addMe);
			else if (group.equalsIgnoreCase("<<triggerer>>"))         sendToUs.add(triggerer);
			else if (group.equalsIgnoreCase("<<command-triggerer>>")) sendToPlayer(message, triggerer, true, false);
			else if (group.equalsIgnoreCase("<<command-recipient>>")) flagCommand = true;
			else if (group.equalsIgnoreCase("<<say-triggerer>>"))     sendToPlayer(message, triggerer, false, true);
			else if (group.equalsIgnoreCase("<<say-recipient>>"))     flagSay     = true;
			//else if (group.equalsIgnoreCase("<<player|rTriggersPlayer>>")) sendToUs.add(makeFakePlayer("rTriggersPlayer", triggerer));
			else if (group.startsWith("<<hasperm|")) sendToPermissions.add(group.substring(10, group.length() - 2));
			else if (group.toLowerCase().startsWith("<<player|"))     sendToUs.add(MCServer.getPlayer(group.substring(9, group.length()-2)));
			else if (group.equalsIgnoreCase("<<command-console>>"))
				for(String command : message.split("\n")) sendToConsole(command, true);
			else if (group.toLowerCase().startsWith("<<craftirc|") && rTriggers.plugin.CraftIRCPlugin != null)
				rTriggers.plugin.CraftIRCPlugin.sendMessageToTag(message, group.substring(11, group.length()-2));
			else if (group.equalsIgnoreCase("<<server>>") || group.equalsIgnoreCase("<<console>>")) {
				sendToConsole(message, false);
			}
			else if (group.startsWith("<<onlyinworld|")) onlyHere = MCServer.getWorld(group.substring(14, group.length() - 2));
			else if (group.startsWith("<<inworld|")) sendToUs.addAll(MCServer.getWorld(group.substring(10, group.length() - 2)).getPlayers());
			else if (group.toLowerCase().startsWith("<<near-triggerer|") && triggerer != null){
				int distance = new Integer(group.substring(17, group.length() - 2));
				for (Entity addMe : triggerer.getNearbyEntities(distance, distance, 127))
					if (addMe instanceof Player) sendToUs.add((Player) addMe);
			}
			else if (group.equalsIgnoreCase("<<twitter>>")){
				String [] with    = {"Twitter", "", "", "",""};
				if (rTriggers.plugin.ServerEventsPlugin != null){
					try {
						ServerEvents.displayMessage(rParser.replaceWords(message, replace, with));
					} catch (ClassCastException ex){
						rTriggers.plugin.log.info("[rTriggers] ServerEvents not found!");
					}
				} else  rTriggers.plugin.log.info("[rTriggers] ServerEvents not found!");
			} else if (group.equalsIgnoreCase("<<execute>>")){
				Runtime rt = Runtime.getRuntime();
				rTriggers.plugin.log.info("[rTriggers] Executing:" + message);
				try {rt.exec(message);}
				catch (IOException e) { e.printStackTrace(); }
			}
		}
		/********************************************************
		 * List of non-special case groups has been constructed.
		 * Find all the players who both:
		 * 1) belong to the non-special case groups in "sendToUs"
		 * 2) don't belong to groups listed in "dontSendToUs"
		 * and send the message to them.  */
		dontSendToUs = constructPlayerList(dontSendToGroupsFiltered, dontSendToPermissions, dontSendToUs);
		sendToUs     = constructPlayerList(sendToGroupsFiltered    , sendToPermissions    , sendToUs);
		sendToUs.removeAll(dontSendToUs);
		
		if (onlyHere != null) sendToUs.retainAll(onlyHere.getPlayers());
		

		for (Player sendToMe : sendToUs) {
			sendToPlayer(message, sendToMe, flagCommand, flagSay);
		}
	}
	/**
	 * @param groups A set of group names
	 * @param permissions A set of permission node names 
	 * @param players A set of players
	 * @return A set containing players from the players set and players who either are members of one of the groups or have one of the permissions.
	 */
	public Set<Player> constructPlayerList(Set<String> groups, Set<String> permissions, Set<Player> players){
		building_the_list:
		for (Player addMe: rTriggers.plugin.getServer().getOnlinePlayers()){
			if (players.contains(addMe)) continue;
			for(String oneOfUs : groups){
				if (rTriggers.plugin.permAdaptor.isInGroup(addMe, oneOfUs)){
					players.add(addMe);
					continue building_the_list;
				}
			}
			for (String perm : permissions) {
				if (rTriggers.plugin.permAdaptor.hasPermission(addMe, perm)){
					players.add(addMe);
					continue building_the_list;
				}
			}
		}
		return players;
	}
	
	public void sendToPlayer(String message, Player recipient, boolean flagCommand, boolean flagSay) {
		// Recursion!
		if (message.contains("<<everyone>>")) {
			for (Player addMe : rTriggers.plugin.getServer().getOnlinePlayers())
			{
				String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
				sendToPlayer(newMessage, recipient, flagCommand, flagSay);
			}
			return;
		}
		// More recursion!
		int index = message.indexOf("<<hasperm|");
		if (index != - 1) {
			index += "<<hasperm|".length();
			int endIndex = message.indexOf(">>", index);
			String perm = message.substring(index, endIndex);
			for (Player addMe: rTriggers.plugin.getServer().getOnlinePlayers()){
				if (rTriggers.plugin.permAdaptor.hasPermission(addMe, perm)){
					String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
					sendToPlayer(newMessage, recipient, flagCommand, flagSay);					
				}
			}
			return;
		}
		
		String [] with = rTriggers.plugin.formatter.getTagReplacements(recipient);
		String [] replace = {"<<recipient>>", "<<recipient-ip>>", "<<recipient-locale>>", "<<recipient-country>>", "<<recipient-balance>>"};
		message = rParser.parseMessage(message, replace, with);
		if (flagSay)
			for(String sayThis : message.split("\n")) recipient.chat(sayThis);
		if (!flagCommand && !flagSay)
			for(String sendMe  : message.split("\n")) recipient.sendMessage(sendMe);
		if (flagCommand)
			for(String command : message.replaceAll("§.", "").split("\n")) rTriggers.plugin.getServer().dispatchCommand(recipient, command); 
	}
	
	public void sendToConsole(String message, boolean isCommand) {
		// Recursion!
		if (message.contains("<<everyone>>")) {
			for (Player addMe : rTriggers.plugin.getServer().getOnlinePlayers())
			{
				String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
				sendToConsole(newMessage, isCommand);
			}
			return;
		}
		// More recursion!
		int index = message.indexOf("<<hasperm|");
		if (index != - 1) {
			index += "<<hasperm|".length();
			int endIndex = message.indexOf(">>", index);
			String perm = message.substring(index, endIndex);
			for (Player addMe: rTriggers.plugin.getServer().getOnlinePlayers()){
				if (rTriggers.plugin.permAdaptor.hasPermission(addMe, perm)){
					String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
					sendToConsole(newMessage, isCommand);					
				}
			}
			return;
		}
		
		Server MCServer = rTriggers.plugin.getServer();
		if (isCommand) {
			String command = message.replaceAll("§.", "");
			MCServer.getPluginManager().callEvent( new ServerCommandEvent(MCServer.getConsoleSender(), command));
			MCServer.dispatchCommand(MCServer.getConsoleSender(), command);
		} else {
			final String [] replace = {"<<recipient>>", "<<recipient-displayname>>", "<<recipient-ip>>", "<<recipient-color>>", "<<recipient-balance>>", "§"};
			final String [] with    = {"server", "", "", "", "§", ""};
			rTriggers.plugin.log.info(rParser.replaceWords(message, replace, with));
		}
	}
}
