package com.reil.bukkit.rTriggers;

import java.awt.print.Paper;
import java.io.*;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.*;

import javax.persistence.PersistenceException;

import org.bukkit.entity.*;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.Event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.command.*;

// Plugin hooking
import org.bukkit.croemmich.serverevents.ServerEvents;
import com.ensifera.animosity.craftirc.CraftIRC;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Methods;
import com.reil.bukkit.rParser.rParser;
import com.reil.bukkit.rTriggers.persistence.LimitTracker;
import com.reil.bukkit.rTriggers.persistence.TriggerLimit;

// Fake Player
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.NetServerHandler;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;

@SuppressWarnings("unused")
public class rTriggers extends JavaPlugin {
	private ConsoleCommandSender Console;
	private boolean registered = false;
	public BukkitScheduler bukkitScheduler;
	public PluginManager pluginManager;
	public rPropertiesFile Messages;
	public Server MCServer;
	public Random RNG;
	public Logger log;
	private static TimeZone timeZone;
	
	public static String commaSplit = "[ \t]*,[ \t]*";
	public static String colonSplit = "[ \t]*:[ \t]*";
	
	rTriggersServerListener serverListener = new rTriggersServerListener(this);
	PlayerListener playerListener = new rTriggersPlayerListener(this);
	EntityListener entityListener = new rTriggersEntityListener(this);
	
	public Method economyPlugin = null;
	public Methods economyMethods;
	public CraftIRC CraftIRCPlugin;
	public PermissionHandler PermissionsPlugin;
	public Plugin ServerEventsPlugin;
    
	TimeKeeper clock;
	LimitTracker limitTracker;
    Map <String, Integer> listTracker = new HashMap<String,Integer>();
	Map <Integer, EntityDamageEvent.DamageCause> deathCause = new HashMap <Integer, EntityDamageEvent.DamageCause>();
	Map <Integer, Player> deathBringer = new HashMap <Integer, Player>();
	public Map <String, HashSet<String>> optionsMap = new HashMap <String, HashSet<String>>();
	List<String> permissionTriggerers = new LinkedList<String>();

	/*
	 * START: Functions for setting up the plugin here 
	 */
	
	
    public void onEnable(){
		economyMethods = new Methods();
		log = Logger.getLogger("Minecraft");
		RNG = new Random();
		MCServer = getServer();
		bukkitScheduler = MCServer.getScheduler();
		pluginManager = MCServer.getPluginManager();
		Console = new ConsoleCommandSender(MCServer);
		getDataFolder().mkdir();
        Messages = new rPropertiesFile(getDataFolder().getPath() + "/rTriggers.properties");
        clock = new TimeKeeper(this, bukkitScheduler, 0);
        limitTracker = new LimitTracker(this);

        int largestDelay = 0;
		try {
			grabPlugins(pluginManager);
			largestDelay = processOptions(Messages.load());
			for (String key : Messages.getKeys()){
				if (key.startsWith("<<hasperm|") || key.startsWith("not|<<hasperm|")) permissionTriggerers.add(key.substring(key.indexOf("|") + 1,key.length() - 2));
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "[rTriggers]: Exception while loading properties file.", e);
		}
		generateTimers(Messages);
		if (optionsMap.containsKey("delay")) {
			setupDatabase();
			log.info("[rTriggers] Cleaned " + limitTracker.cleanEntriesOlderThan(largestDelay) + " entries from delay persistence table");
		}
		if (Messages.keyExists("s:timezone")){
			timeZone = new SimpleTimeZone(Messages.getInt("s:timezone")*3600000, "Server Time");
		} else timeZone = TimeZone.getDefault();
		
		// Do onload events for everything that might have loaded before rTriggers
		serverListener.checkAlreadyLoaded(pluginManager);
		
		log.info("[rTriggers] Loaded: Version " + getDescription().getVersion());
	}
	
	/**
	 * Goes through each message in messages[] and registers events that it sees in each.
	 * @param messages
	 */
	public int processOptions(String[] messages){
		int largestLimit = 0;
		if (registered) return 0;
		else registered = true;
		
		boolean [] flag = new boolean[7];
		Arrays.fill(flag, false);
		PluginManager manager = MCServer.getPluginManager();
		
		for(String message : messages){
			String [] split = message.split(colonSplit, 3);
			if (!(split.length >= 2)) continue;
			
			String options = split[1];
			if(!flag[0] && (options.isEmpty() || options.contains("onlogin"))){
				manager.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
				flag[0] = true;
			}
			if(!flag[1] && options.contains("ondisconnect")){
				manager.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
				flag[1] = true;
			}
			if(!flag[2] && options.contains("oncommand")){
				manager.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Monitor, this);
				flag[2] = true;
			}
			if(!flag[3] && options.contains("onkick")){
				manager.registerEvent(Event.Type.PLAYER_KICK, playerListener, Priority.Monitor, this);
				flag[3] = true;
			}
			if(!flag[4] && options.contains("ondeath")){
				manager.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Monitor, this);
				manager.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Monitor, this);
				flag[4] = true;
			}
			if(!flag[5] && options.contains("onconsole")){
				manager.registerEvent(Event.Type.SERVER_COMMAND, serverListener, Priority.Monitor, this);
				flag[5] = true;
			}
			if(!flag[6] && options.contains("onrespawn")){
				manager.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Monitor, this);
				flag[6] = true;
			}
			if(options.contains("onload")){
				for (String option: options.split(commaSplit)){
					if (option.startsWith("onload|")) {
						String pluginName = option.substring("onload|".length());
						serverListener.listenFor(pluginName);
					}
				}
			}
			
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
				}
				if(!optionsMap.containsKey(option)) optionsMap.put(option, new HashSet<String>());
				optionsMap.get(option).add(message);
			}
		}
		
		// Need these no matter what, so we can hook into other plugins (economy, permissions, CraftIRC, ServerEvents)
		manager.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		manager.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		return largestLimit * 1000;
	}

	/**
	 *  Checks to see if plugins which rTriggers supports have already been loaded.
	 *  Registers rTriggers with already-loaded plugins it finds.
	 */
	public void grabPlugins(PluginManager manager) {
		if (PermissionsPlugin == null && manager.getPlugin("Permissions") != null){
        	PermissionsPlugin = Permissions.Security;
        	log.info("[rTriggers] Attached to Permissions.");
        }
        
        Plugin CraftIRCTry = manager.getPlugin("CraftIRC");
        if (CraftIRCPlugin == null && CraftIRCTry != null){
        	CraftIRCPlugin = (CraftIRC) CraftIRCTry;
        	log.info("[rTriggers] Attached to CraftIRC.");
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
						bukkitScheduler.scheduleAsyncRepeatingTask (this,
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
	
	private void setupDatabase() {
        try {
            getDatabase().find(TriggerLimit.class).findRowCount();
        } catch (PersistenceException ex) {
            System.out.println("[rTriggers] Setting up persistence...");
            installDDL();
        }
    }
   @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(TriggerLimit.class);
        return list;
    }
	
	@Override
	public void onDisable(){
		Messages.save();
		bukkitScheduler.cancelTasks(this);
		log.info("[rTriggers] Disabled!");
	} 
	
	
	/*
	 * Start: Functions for triggering/sending/dispatching messages
	 */
	
	
	/* Looks through all of the messages,
	 * Sends the messages triggered by groups which 'triggerMessage' is a member of,
	 * But only if that message has the contents of 'option' as one of its options */
	public boolean triggerMessages(String option){ return triggerMessages(null, option); }
	public boolean triggerMessages(Player triggerMessage, String option){ return triggerMessages(triggerMessage, option, new String[0], new String[0]);	}
	public boolean triggerMessages(String option, String[] eventToReplace, String []eventReplaceWith){ return triggerMessages(null, option, eventToReplace, eventReplaceWith);}
	
	public boolean triggerMessages(Player triggerer, String option, String[] eventToReplace, String[] eventReplaceWith){
		/* Send all message candidates */
		Set<String> sendThese = getMessages(triggerer, option);
		
		message_rollout:
		for (String fullMessage : sendThese){	
			if (limitTracker.tooSoon(fullMessage, triggerer)) continue; // Don't send messages if they have the limit option and it's been too soon.
			
			String [] split =  fullMessage.split(colonSplit, 3);
			/**************************
			 * Tag replacement start!
			 *************************/
			String message = replaceLists(split[2]);
			
			// Regex's which catch @, but not \@ and &, but not \&
			
			message = stdReplace(message);
			
			String [] replace = { "<<triggerer>>", "<<triggerer-displayname>>", "<<triggerer-ip>>", "<<triggerer-locale>>", "<<triggerer-country>>", "<<triggerer-balance>>" };
			String [] with    = getTagReplacements(triggerer);
			message = rParser.replaceWords(message, replace, with);
			
			if (eventToReplace.length > 0) {
				message = rParser.replaceWords(message, eventToReplace, eventReplaceWith);
				split[0] = rParser.replaceWords(split[0], eventToReplace, eventReplaceWith);
				split[1] = rParser.replaceWords(split[1], eventToReplace, eventReplaceWith);
			}
			/**************************
			 *  Tag replacement end! */
			
			// Ship out the message.  If it has a delay on it, put it on the scheduler
			if (!optionsMap.containsKey("delay") || !optionsMap.get("delay").contains(fullMessage))
				sendMessage(message, triggerer, split[0]);
			else {
				long waitTime = 0;
				for(String checkOption : split[1].split(commaSplit)) {
					if (checkOption.startsWith("delay|")) {
						try{
							waitTime = 20 * Long.parseLong(checkOption.substring(6));
						} catch (NumberFormatException e) {
							log.info("[rTriggers] Bad number format on option: " + checkOption + "\n in message: " + fullMessage);
							continue;
						}
						bukkitScheduler.scheduleAsyncDelayedTask (this,
								new rTriggersTimer(this, split[0] + "::"+message, triggerer),
								waitTime);
					}
				}
			}
		}
		return !sendThese.isEmpty();
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
			if(PermissionsPlugin != null) groupArray.addAll(Arrays.asList(PermissionsPlugin.getGroups(triggerer.getWorld().getName(),triggerer.getName())));
		} else groupArray.add("<<customtrigger>>");
		
		/* Build set of message candidates */
		Set<String> sendThese = new LinkedHashSet<String>();
		for (String groupName : groupArray)
			if(Messages.keyExists(groupName)) sendThese.addAll(Arrays.asList(Messages.getStrings(groupName)));
		if (PermissionsPlugin != null && triggerer != null){
			for(String permission : permissionTriggerers){
				String permString = "<<hasperm|" + permission + ">>";
				if(PermissionsPlugin.has(triggerer, permission)) {
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
		
		String [] replace = {"<<recipient>>", "<<recipient-displayname>>", "<<recipient-ip>>", "<<recipient-color>>", "<<recipient-balance>>", "§"};
		
		Set <String> sendToGroupsFiltered     = new HashSet <String>();
		Set <String> dontSendToGroupsFiltered = new HashSet <String>();
		
		Set <String> sendToPermissions     = new HashSet <String>();
		Set <String> dontSendToPermissions = new HashSet <String>();
		
		Set <Player> sendToUs     = new HashSet<Player>();
		Set <Player> dontSendToUs = new HashSet<Player>();
		dontSendToUs.add(null);
		
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
				} else if(notTarget.startsWith("<<hasperm|")) dontSendToPermissions.add(notTarget.substring(10, notTarget.length() - 2));
			}
			else if (!group.startsWith("<<")) sendToGroupsFiltered.add(group);
			/* Special cases: start! */
			else if (group.equalsIgnoreCase("<<everyone>>"))          for (Player addMe : MCServer.getOnlinePlayers()) sendToUs.add(addMe);
			else if (group.equalsIgnoreCase("<<triggerer>>"))         sendToUs.add(triggerer);
			else if (group.equalsIgnoreCase("<<command-triggerer>>")) sendToPlayer(message, triggerer, true, false);
			else if (group.equalsIgnoreCase("<<command-recipient>>")) flagCommand = true;
			else if (group.equalsIgnoreCase("<<say-triggerer>>"))     sendToPlayer(message, triggerer, false, true);
			else if (group.equalsIgnoreCase("<<say-recipient>>"))     flagSay     = true;
			else if (group.equalsIgnoreCase("<<player|&rTriggers>>")) sendToUs.add(makeFakePlayer("&rTriggers", triggerer));
			else if (group.startsWith("<<hasperm|")) sendToPermissions.add(group.substring(10, group.length() - 2));
			else if (group.toLowerCase().startsWith("<<player|"))     sendToUs.add(MCServer.getPlayer(group.substring(9, group.length()-2)));
			else if (group.equalsIgnoreCase("<<command-console>>"))
				for(String command : message.split("\n")) MCServer.dispatchCommand(Console, command.replaceAll("§.", ""));
			else if (group.toLowerCase().startsWith("<<craftirc|") && CraftIRCPlugin != null)
				CraftIRCPlugin.sendMessageToTag(message, group.substring(11, group.length()-2));
			else if (group.equalsIgnoreCase("<<server>>") || group.equalsIgnoreCase("<<console>>")) {
				String [] with    = {"server", "", "", "", "§", "",};
				log.info("[rTriggers] " + rParser.replaceWords(message, replace, with));
			}
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
				try {Process pr = rt.exec(message);}
				catch (IOException e) { e.printStackTrace(); }
			}
		}
		/****************************************************
		 * List of non-special case groups has been constructed.
		 * Find all the  players who belong to the non-special
		 * case groups, and send the message to them.  */
		dontSendToUs = constructPlayerList(dontSendToGroupsFiltered, dontSendToPermissions, dontSendToUs);
		sendToUs     = constructPlayerList(sendToGroupsFiltered    , sendToPermissions    , sendToUs);
		sendToUs.removeAll(dontSendToUs);
		
		for (Player sendToMe : sendToUs) sendToPlayer(message, sendToMe, flagCommand, flagSay);
	}
	/**
	 * @param groups A set of group names
	 * @param permissions A set of permission node names 
	 * @param players A set of players
	 * @return A set containing players from the players set and players who either are members of one of the groups or have one of the permissions.
	 */
	public Set<Player> constructPlayerList(Set<String> groups, Set<String> permissions, Set<Player> players){
		if (PermissionsPlugin == null) return players;
		
		building_the_list:
		for (Player addMe: MCServer.getOnlinePlayers()){
			if (players.contains(addMe)) continue;
			for(String oneOfUs : groups){
				if (PermissionsPlugin.inSingleGroup(addMe.getWorld().getName(), addMe.getName(), oneOfUs)){
					players.add(addMe);
					continue building_the_list;
				}
			}
			for (String perm : permissions) {
				if (PermissionsPlugin.has(addMe, perm)){
					players.add(addMe);
					continue building_the_list;
				}
			}
		}
		return players;
	}
	
	public void sendToPlayer(String message, Player recipient, boolean flagCommand, boolean flagSay) {
		String [] with = getTagReplacements(recipient);
		String [] replace = {"<<recipient>>", "<<recipient-ip>>", "<<recipient-locale>>", "<<recipient-country>>", "<<recipient-balance>>"};
		message = rParser.parseMessage(message, replace, with);
		if (flagSay)
			for(String sayThis : message.split("\n")) recipient.chat(sayThis);
		if (!flagCommand && !flagSay)
			for(String sendMe  : message.split("\n")) recipient.sendMessage(sendMe);
		if (flagCommand && !recipient.getName().equals("&rTriggers"))
			for(String command : message.split("\n")) recipient.performCommand(command.replaceAll("§.", ""));
		if (flagCommand &&  recipient.getName().equals("&rTriggers"))
			for(String command : message.split("\n")) recipient.chat("/" + command.replaceAll("§.", ""));
	}

	/*
	 * Start: Functions for formatting messages
	 */
	
	/* Takes care of replacements that don't vary per player. */
	public static String stdReplace(String message) {
		Calendar time = Calendar.getInstance(timeZone);
		String minute = String.format("%2d", time.get(Calendar.MINUTE));
		String hour   = Integer.toString(time.get(Calendar.HOUR));
		String hour24 = String.format("%2d", time.get(Calendar.HOUR_OF_DAY));
		String [] replace = {"(?<!\\\\)@", "(?<!\\\\)&", "<<color>>","<<time>>"         ,"<<time\\|24>>"        ,"<<hour>>", "<<minute>>"};
		String [] with    = {"\n§f"      , "§"         , "§"        ,hour + ":" + minute,hour24 + ":" + minute, hour     , minute};
		message = rParser.replaceWords(message, replace, with);
		return message;
	}

	/*
	 * Will replace user-generated lists, as well as the player list.
	 */
	public String replaceLists(String message) {
		int optionStart;
		int optionEnd;
		String listMember;
		
		// Replace user-generated lists:
		while ( (optionStart = message.indexOf("<<list|") + 7)     !=  6 &&
				(optionEnd   = message.indexOf(">>", optionStart)) != -1){
			String options = message.substring(optionStart, optionEnd);
			String [] optionSplit = options.split("\\|");
			String [] messageList = Messages.getStrings("<<list|" + optionSplit[0] + ">>");
			
			if (messageList.length > 0){
				if (!(optionSplit.length > 1) || !optionSplit[1].equalsIgnoreCase("rand")){
					if(!listTracker.containsKey(optionSplit[0]))
						listTracker.put(optionSplit[0], 0);
					int listNumber = listTracker.get(optionSplit[0]);
					listMember = messageList[listNumber];
					listTracker.put(optionSplit[0], (listNumber + 1)%messageList.length);
				} else listMember = messageList[RNG.nextInt(messageList.length)];
			} else listMember = "";
			message = message.replace("<<list|" + options + ">>", listMember);
		}
		
		// Now replace any use of <<player-list>>
		if(message.contains("<<player-list>>")){
			StringBuilder list = new StringBuilder();
			String prefix = "";
			
			for (Player getName : MCServer.getOnlinePlayers()){
				list.append(prefix + getName.getDisplayName());
				prefix = ", ";
			}
			message = message.replaceAll("<<player-list>>", list.toString());
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
		if (player == null || player.getName().equals("&rTriggers")){
			String [] returnArray = {"", "", "", "", "", ""};
			return returnArray;
		}
		// Get balance tag
		double balance = 0;
		if (economyPlugin != null && economyPlugin.hasAccount(player.getName()))
			balance = economyPlugin.getAccount(player.getName()).balance();
		
		// Get ip and locale tags
		InetSocketAddress IP = player.getAddress();
		String country;
		String locale;
		try {
			Locale playersHere = net.sf.javainetlocator.InetAddressLocator.getLocale(IP.getAddress());
			country = playersHere.getDisplayCountry();
			locale = playersHere.getDisplayName();
		} catch (Exception e){
			country = ""; 
			locale = "";
		}
		String [] returnArray = { player.getName(), player.getDisplayName(), IP.toString(), locale, country, Double.toString(balance)};
		return returnArray;
	}
	
	public static String damageCauseNatural(EntityDamageEvent.DamageCause causeOfDeath){
		switch (causeOfDeath) {
		case CONTACT:
			return "touching something";
		case ENTITY_ATTACK:
			return "being hit";
		case SUFFOCATION:
			return "suffocation";
		case FALL:
			return "falling";
		case FIRE:
			return "fire";
		case FIRE_TICK:
			return "burning";
		case LAVA:
			return "lava";
		case DROWNING:
			return "drowning";
		case BLOCK_EXPLOSION:
			return "explosion";
		case ENTITY_EXPLOSION:
			return "creeper";
		case CUSTOM:
			return "the unknown";
		case LIGHTNING:
			return "lighning";
		default:
			return "something";
		}
	}
	
	public Player makeFakePlayer(String Name, Player player) {
		CraftServer cServer = (CraftServer) MCServer;
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
        
        return fakePlayer;
	}
	
	public static Entity getEntityById(int entityId, World world){
	    net.minecraft.server.Entity ent = ((CraftWorld)world).getHandle().getEntity(entityId);
	    if ((ent != null) && ((ent instanceof EntityLiving)))
	        return ent.getBukkitEntity();
	    else return null;
	}
	
	public static String getName(Entity thisGuy){
		if (thisGuy instanceof Player)
			return ((Player)thisGuy).getName();
		String targeterName = thisGuy.getClass().getName();
		return targeterName.substring(targeterName.lastIndexOf("Craft") + "Craft".length());
	}
}