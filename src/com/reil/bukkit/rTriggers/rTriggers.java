package com.reil.bukkit.rTriggers;

import java.io.*;
import java.net.InetSocketAddress;
import net.sf.javainetlocator.InetAddressLocator;
import java.util.*;
import java.util.logging.*;

import javax.persistence.PersistenceException;

import org.bukkit.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

// Plugin hooking
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.croemmich.serverevents.ServerEvents;
import com.ensifera.animosity.craftirc.CraftIRC;
import com.nijikokun.register.payment.Methods;
import com.reil.bukkit.rParser.rParser;
import com.reil.bukkit.rTriggers.listener.CommandListener;
import com.reil.bukkit.rTriggers.listener.EventListener;
import com.reil.bukkit.rTriggers.listener.SetupListener;
import com.reil.bukkit.rTriggers.persistence.LimitTracker;
import com.reil.bukkit.rTriggers.persistence.TriggerLimit;
import com.reil.bukkit.rTriggers.timers.TimeKeeper;
import com.reil.bukkit.rTriggers.timers.rTriggersTimer;


public class rTriggers extends JavaPlugin {
	public Random RNG;
	public Logger log = Logger.getLogger("Minecraft");
	public rPropertiesFile Messages;
	
	private boolean registered;
	public boolean useRegister;
	public boolean useiNetLocator;
	
	public static final String commaSplit = "[ \t]*,[ \t]*";
	public static final String colonSplit = "[ \t]*:[ \t]*";
	private static TimeZone timeZone;
	
	private SetupListener serverListener = new SetupListener(this);
	private Listener playerListener = new EventListener(this);
	private CommandListener commandListener = new CommandListener(this);

	public CraftIRC CraftIRCPlugin;
	public PermissionsAdaptor permAdaptor;
	public Plugin ServerEventsPlugin;
    
	public TimeKeeper clock;
	public LimitTracker limitTracker;
	public Map <String, Integer> listTracker;
	public Map <String, HashSet<String>> optionsMap;
	List<String> permissionTriggerers;

	@Override
	public void onEnable(){
		RNG = new Random();
		getDataFolder().mkdir();
        Messages = new rPropertiesFile(getDataFolder().getPath() + "/rTriggers.properties");
        clock = new TimeKeeper(this, getServer().getScheduler(), 0);
        limitTracker = new LimitTracker(this);
        
        listTracker = new HashMap<String,Integer>();
        optionsMap = new HashMap<String, HashSet<String>>();
        permissionTriggerers = new LinkedList<String>();     
        
        registered = false;
        
		
        int largestDelay = 0;
        
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(serverListener, this);
		manager.registerEvents(playerListener, this);
		manager.registerEvents(commandListener, this);
		
        grabPlugins(manager);
        commandListener.clearMaps();

        
        // - Loading the rTriggers.properties file.
        // - picking out and handling messages with onload and limit options.
        // - Picking out permissions that trigger.
		try {
			largestDelay = processOptions(Messages.load());
			for (String key : Messages.getKeys()){
				if (key.startsWith("<<hasperm|") || key.startsWith("not|<<hasperm|")) permissionTriggerers.add(key.substring(key.lastIndexOf("|") + 1,key.length() - 2));
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "[rTriggers]: Exception while loading properties file.", e);
		}
		generateTimers(Messages);
		
		
		// Set up the database if needed (only needed for delays)
		if (optionsMap.containsKey("delay")) {
	        try {
	            getDatabase().find(TriggerLimit.class).findRowCount();
	        } catch (PersistenceException ex) {
	            System.out.println("[rTriggers] Setting up persistence...");
	            installDDL();
	        }
			log.info("[rTriggers] Cleaned " + limitTracker.cleanEntriesOlderThan(largestDelay) + " entries from delay persistence table");
		}
		
		// Special settings line: timezone
		if (Messages.keyExists("s:timezone")){
			timeZone = new SimpleTimeZone(Messages.getInt("s:timezone")*3600000, "Server Time");
		} else timeZone = TimeZone.getDefault();
		
		// Do onload events for everything that might have loaded before rTriggers
		serverListener.checkAlreadyLoaded(getServer().getPluginManager());
		
		log.info("[rTriggers] Loaded: Version " + getDescription().getVersion());
	}
	
	@Override
	public void onDisable(){
		Messages.save();
		getServer().getScheduler().cancelTasks(this);
		log.info("[rTriggers] Disabled!");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("rtriggers")){
			if(args.length >= 1){
				if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("rtriggers.admin.reload")){
					getServer().getPluginManager().disablePlugin(this);
					getServer().getPluginManager().enablePlugin(this);
				}
				if (args[0].equalsIgnoreCase("list") && sender.hasPermission("rtriggers.admin.list")){
					for(String key: Messages.getKeys()){
						sender.sendMessage(key + ":");
						sender.sendMessage(Messages.getStrings(key));
					}
				}
			}
		}
		return true;
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
	    List<Class<?>> list = new ArrayList<Class<?>>();
	    list.add(TriggerLimit.class);
	    return list;
	}

	/**
	 * Goes through each message in messages[] and registers events that it sees in each.
	 * @param messages
	 */
	public int processOptions(String[] messages){
		int largestLimit = 0;
		if (registered) return 0;
		else registered = true;
		
		boolean [] flag = new boolean[9];
		Arrays.fill(flag, false);
		
		for(String message : messages){
			String [] split = message.split(colonSplit, 3);
			if (!(split.length >= 2)) continue;
			
			String options = split[1];
			
			if(options.contains("onload")){
				for (String option: options.split(commaSplit)){
					if (option.startsWith("onload|")) {
						String pluginName = option.substring("onload|".length());
						serverListener.listenFor(pluginName);
					}
				}
			}
			
			
			// Places all messages in sets which correspond to each of the message's options.
			if (options.isEmpty()) options = "onlogin";
			for(String option : options.split(commaSplit)){
				if(option.startsWith("limit|")){
					int indexEnd = option.lastIndexOf('|');
					if (indexEnd == 5) indexEnd = option.length();
					int limitTime = Integer.parseInt(option.substring(6, indexEnd));
					if (limitTime > largestLimit) largestLimit = limitTime;
					if (option.endsWith("perTrigger")) option = "limit|perTrigger";
					else option = "limit";
				} else if (option.startsWith("delay|")) {
					option = "delay";
				} else if (option.startsWith("oncommand|"))	{
					// TODO
					commandListener.addCommand(option);
				} else if (option.startsWith("onconsole|")) {
					// TODO
					commandListener.addConsoleCommand(option);
				}
				if(!optionsMap.containsKey(option)) optionsMap.put(option, new HashSet<String>());
				optionsMap.get(option).add(message);
			}
		}
		return largestLimit * 1000;
	}

	/**
	 *  Checks to see if any rTriggers-supported plugins	 have already been loaded.
	 *  Registers rTriggers with already-loaded plugins it finds.
	 */
	public void grabPlugins(PluginManager manager) {
		// Checking for permissions plugins
		permAdaptor = new PermissionsAdaptor(this);
		
		// Checking for CraftIRC
        Plugin CraftIRCTry = manager.getPlugin("CraftIRC");
        if (CraftIRCPlugin == null && CraftIRCTry != null){
        	CraftIRCPlugin = (CraftIRC) CraftIRCTry;
        	log.info("[rTriggers] Attached to CraftIRC.");
        }
        
        // Checking to see if they've got Register or InetAddressLocator
        try {
			Class.forName ("com.nijikokun.register.payment.Methods");
			useRegister = true;
			log.info("[rTriggers] Register found.");
		}
		catch (ClassNotFoundException e)
		{
			useRegister = false;
		}
        
        // Checking to see if they have InetAddressLocator.
		try {
			Class.forName ("net.sf.javainetlocator.InetAddressLocator");
			useiNetLocator = true;
			log.info("[rTriggers] InetAddressLocator found.");
		}
		catch (ClassNotFoundException e)
		{
			useiNetLocator = false;
		}
	}
	
	/*
	 * Precondition: We already have messages loaded
	 * Postcondition: New threads for each timer have been created.  
	 */
	public void generateTimers(rPropertiesFile messages){
		for(String key : messages.getKeys()){
			try {
				if (key.startsWith("<<timer|")){
					for(String message : messages.getStrings(key)){
						long waitTime = 20 * new Long(key.substring(8, key.length()-2));
						getServer().getScheduler().scheduleSyncRepeatingTask (this,
								new rTriggersTimer(this, message),
								waitTime, waitTime);
					}
				}
			} catch (NumberFormatException e){
				log.log(Level.WARNING, "[rTriggers] Invalid number string:" + key);
			}
		}
		if (messages.keyExists("<<timer>>")) log.log(Level.WARNING, "[rTriggers] Using old timer format! Please update to new version.");
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
			
			message = replaceCustomLists(split[2]);
			message = replaceGeneratedLists(message);
			
			// Regex's which catch @, but not \@ and &, but not \&
			
			message = stdReplace(message);
			
			final String [] replace = { "<<triggerer>>", "<<triggerer-displayname>>", "<<triggerer-ip>>", "<<triggerer-locale>>", "<<triggerer-country>>", "<<triggerer-balance>>", };
			message = rParser.replaceWords(message, replace, getTagReplacements(triggerer));
			
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
						log.info("[rTriggers] Bad number format on option: " + checkOption + "\n in message: " + fullMessage);
						continue;
					}
					// Note, this doesn't actually -remove- the entire delay option, it just reduces it to a number, which the option parser should ignore.
					fullMessage = split[0] + ":" + split[1].replaceAll("delay|","") + ":" + message;
					getServer().getScheduler().scheduleSyncDelayedTask (this,
							new rTriggersTimer(this, fullMessage , triggerer),
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
			groupArray.addAll(permAdaptor.getGroups(triggerer));
			if(triggerer.isOp()){
				groupArray.add("<<ops>>");
				groupArray.add("<<op>>");
			}
		} else groupArray.add("<<customtrigger>>");
		
		/* Build set of message candidates */
		Set<String> sendThese = new LinkedHashSet<String>();
		for (String groupName : groupArray)
			if(Messages.keyExists(groupName)) sendThese.addAll(Arrays.asList(Messages.getStrings(groupName)));
		if (triggerer != null){
			for(String permission : permissionTriggerers){
				String permString = "<<hasperm|" + permission + ">>";
				if(permAdaptor.hasPermission(triggerer,permission)) {
					if (Messages.keyExists(permString)) sendThese.addAll(Arrays.asList(Messages.getStrings(permString)));
				} else {
					if (Messages.keyExists("not|" + permString)) sendThese.addAll(Arrays.asList(Messages.getStrings("not|" + permString)));
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
		
		final String [] replace = {"<<recipient>>", "<<recipient-displayname>>", "<<recipient-ip>>", "<<recipient-color>>", "<<recipient-balance>>", "�"};
		
		Set <String> sendToGroupsFiltered     = new HashSet <String>();
		Set <String> dontSendToGroupsFiltered = new HashSet <String>();
		
		Set <String> sendToPermissions     = new HashSet <String>();
		Set <String> dontSendToPermissions = new HashSet <String>();
		
		Set <Player> sendToUs     = new HashSet<Player>();
		Set <Player> dontSendToUs = new HashSet<Player>();
		dontSendToUs.add(null);
		
		World onlyHere = null;
		Server MCServer = getServer();
		
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
			else if (group.toLowerCase().startsWith("<<craftirc|") && CraftIRCPlugin != null)
				CraftIRCPlugin.sendMessageToTag(message, group.substring(11, group.length()-2));
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
				if (ServerEventsPlugin != null){
					try {
						ServerEvents.displayMessage(rParser.replaceWords(message, replace, with));
					} catch (ClassCastException ex){
						log.info("[rTriggers] ServerEvents not found!");
					}
				} else  log.info("[rTriggers] ServerEvents not found!");
			} else if (group.equalsIgnoreCase("<<execute>>")){
				Runtime rt = Runtime.getRuntime();
				log.info("[rTriggers] Executing:" + message);
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
		for (Player addMe: getServer().getOnlinePlayers()){
			if (players.contains(addMe)) continue;
			for(String oneOfUs : groups){
				if (permAdaptor.isInGroup(addMe, oneOfUs)){
					players.add(addMe);
					continue building_the_list;
				}
			}
			for (String perm : permissions) {
				if (permAdaptor.hasPermission(addMe, perm)){
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
			for (Player addMe : getServer().getOnlinePlayers())
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
			for (Player addMe: getServer().getOnlinePlayers()){
				if (permAdaptor.hasPermission(addMe, perm)){
					String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
					sendToPlayer(newMessage, recipient, flagCommand, flagSay);					
				}
			}
			return;
		}
		
		String [] with = getTagReplacements(recipient);
		String [] replace = {"<<recipient>>", "<<recipient-ip>>", "<<recipient-locale>>", "<<recipient-country>>", "<<recipient-balance>>"};
		message = rParser.parseMessage(message, replace, with);
		if (flagSay)
			for(String sayThis : message.split("\n")) recipient.chat(sayThis);
		if (!flagCommand && !flagSay)
			for(String sendMe  : message.split("\n")) recipient.sendMessage(sendMe);
		if (flagCommand)
			for(String command : message.replaceAll("�.", "").split("\n")) getServer().dispatchCommand(recipient, command); 
	}
	
	public void sendToConsole(String message, boolean isCommand) {
		// Recursion!
		if (message.contains("<<everyone>>")) {
			for (Player addMe : getServer().getOnlinePlayers())
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
			for (Player addMe: getServer().getOnlinePlayers()){
				if (permAdaptor.hasPermission(addMe, perm)){
					String newMessage = message.replaceAll("<<everyone>>", addMe.getName());
					sendToConsole(newMessage, isCommand);					
				}
			}
			return;
		}
		
		Server MCServer = getServer();
		if (isCommand) {
			String command = message.replaceAll("�.", "");
			MCServer.getPluginManager().callEvent( new ServerCommandEvent(MCServer.getConsoleSender(), command));
			MCServer.dispatchCommand(MCServer.getConsoleSender(), command);
		} else {
			final String [] replace = {"<<recipient>>", "<<recipient-displayname>>", "<<recipient-ip>>", "<<recipient-color>>", "<<recipient-balance>>", "�"};
			final String [] with    = {"server", "", "", "", "�", ""};
			log.info(rParser.replaceWords(message, replace, with));
		}
	}

	/*
	 * Start: Functions for formatting messages
	 */
	
	/* Takes care of replacements that don't vary per player. */
	public static String stdReplace(String message) {
		Calendar time = Calendar.getInstance(timeZone);
		String minute = String.format("%tM", time);
		String hour   = Integer.toString(time.get(Calendar.HOUR));
		String hour24 = String.format("%tH", time);
		String [] replace = {"(?<!\\\\)@", "(?<!\\\\)&", "<<color>>","\\\\&", "\\\\@", "<<time>>"          ,"<<time\\|24>>"        ,"<<hour>>", "<<minute>>", "<<player-count>>"};
		String [] with    = {"\n�f"      , "�"         , "�"        ,"&"    , "@"    , hour + ":" + minute,hour24 + ":" + minute  , hour     , minute      , Integer.toString(Bukkit.getServer().getOnlinePlayers().length)};
		message = rParser.replaceWords(message, replace, with);
		return message;
	}

	/*
	 * Will replace user-generated lists, as well as the player list.
	 */
	public String replaceCustomLists(String message) {
		int optionStart;
		int optionEnd;
		String listMember;
		
		// Replace user-generated lists:
		while ( (optionStart = message.indexOf("<<list|") + 7)     !=  6 &&
				(optionEnd   = message.indexOf(">>", optionStart)) != -1){
			String options = message.substring(optionStart, optionEnd);
			String [] optionSplit = options.split("\\|");
			String [] messageList = Messages.getStrings("<<list|" + optionSplit[0] + ">>");
			
			if (messageList.length != 0){
				int listNumber;
				if (optionSplit.length == 1){
					if(!listTracker.containsKey(optionSplit[0]))
						listTracker.put(optionSplit[0], 0);
					listNumber = listTracker.get(optionSplit[0]);
					listTracker.put(optionSplit[0], (listNumber + 1)%messageList.length);
				} else if (optionSplit[1].equalsIgnoreCase("rand")) {
					listNumber = RNG.nextInt(messageList.length);
				}
				else { 
					try {
						listNumber = Integer.parseInt(optionSplit[1]);
					}
					catch (NumberFormatException e) {
						listNumber = 0;
					}
				}
				listMember = messageList[listNumber]; 
			} else listMember = "";
			message = message.replace("<<list|" + options + ">>", listMember);
		}
		return message;
	}
	
	
	public String replaceGeneratedLists(String message){
		if(message.contains("<<player-list>>") || message.contains("<<sleep-list>>") || message.contains("<<nosleep-list>>")){
			StringBuilder list = new StringBuilder();
			StringBuilder sleepList = new StringBuilder();
			StringBuilder notSleepList = new StringBuilder();
			String prefix = "", sleepPrefix = "", notSleepPrefix = "";
			
			for (Player getName : getServer().getOnlinePlayers()){
				String name = getName.getDisplayName();
				list.append(prefix + name);
				prefix = ", ";
				if (getName.isSleeping()){
					sleepList.append(sleepPrefix + name);
					sleepPrefix = ", ";
				} else {
					notSleepList.append(notSleepPrefix + name);
					notSleepPrefix = ", ";
				}
			}
			String [] replace = {"<<player-list>>", "<<sleep-list>>", "<<nosleep-list>>"};
			String [] with    = {list.toString()  , sleepList.toString(), notSleepList.toString()};
			message = rParser.replaceWords(message, replace, with);
		}
		return message;
	}
	/**
	 * Use in conjunction with rParser.replaceWords or rParser.parseMessage;
	 * @param player A player to get the replacements for
	 * @return Array of things to replace tags in this order:
	 *         Name, Display Name, IP address, locale, country, iConomy balance
	 */
	public String[] getTagReplacements(Player player){
		if (player == null || player.getName().equals("rTriggersPlayer")){
			String [] returnArray = {"", "", "", "", "", ""};
			return returnArray;
		}
		// Get balance tag
		double balance = 0;
		if (useRegister && Methods.getMethod() != null && Methods.getMethod().hasAccount(player.getName()))
			balance = Methods.getMethod().getAccount(player.getName()).balance();
		
		// Get ip and locale tags
		InetSocketAddress IP = player.getAddress();
		String country = "";
		String locale = "";
		String IPString;
		
		try {
			if (useiNetLocator) {
				Locale playersHere = InetAddressLocator.getLocale(IP.getAddress());
				country = playersHere.getDisplayCountry();
				locale = playersHere.getDisplayName();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
		try {
			IPString = IP.toString();
		} catch (Exception e){
			IPString = "";
		}
		String [] returnArray = { player.getName(), player.getDisplayName(), IPString, locale, country, Double.toString(balance)};
		return returnArray;
	}
	
	public Player makeFakePlayer(String Name, Player player) {
		/*
		CraftServer cServer = (CraftServer) getServer();
        CraftWorld cWorld = (CraftWorld) player.getWorld();
        EntityPlayer fakeEntityPlayer = new EntityPlayer(
                        cServer.getHandle().server, cWorld.getHandle(),
                        Name, new ItemInWorldManager(cWorld.getHandle()));
        
        fakeEntityPlayer.netServerHandler = ((CraftPlayer) player).getHandle().netServerHandler;
        
        NetServerHandler playerNSH = ((CraftPlayer)player).getHandle().netServerHandler;
        FakeNetServerHandler fakeNSH = new FakeNetServerHandler(cServer.getServer(), playerNSH.networkManager, fakeEntityPlayer);
        playerNSH.networkManager.a(playerNSH);
        fakeEntityPlayer.netServerHandler = fakeNSH;
        
        Player fakePlayer = (Player) fakeEntityPlayer.getBukkitEntity();
        fakePlayer.setDisplayName(Name);
        */
        
        return null;
	}
	
	public static String damageCauseNatural(EntityDamageEvent.DamageCause causeOfDeath){
		switch (causeOfDeath) {
		case CONTACT:
			return "touching something";
		case ENTITY_ATTACK:
			return "being hit";
		case FALL:
			return "falling";
		case FIRE_TICK:
			return "burning";
		case BLOCK_EXPLOSION:
			return "explosion";
		case ENTITY_EXPLOSION:
			return "creeper";
		case CUSTOM:
			return "the unknown";
		case VOID:
			return "falling into the void";
		default:
			return causeOfDeath.toString().toLowerCase();
		}
	}
	
	public static String getName(Entity thisGuy){
		if (thisGuy instanceof Player)
			return ((Player)thisGuy).getName();
		String targeterName = thisGuy.getClass().getName();
		return targeterName.substring(targeterName.lastIndexOf("Craft") + "Craft".length());
	}
}