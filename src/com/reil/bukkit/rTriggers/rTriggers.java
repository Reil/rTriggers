package com.reil.bukkit.rTriggers;

import java.util.*;
import java.util.logging.*;

import javax.persistence.PersistenceException;

import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

// Plugin hooking
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.ensifera.animosity.craftirc.CraftIRC;

import com.reil.bukkit.rTriggers.listener.CommandListener;
import com.reil.bukkit.rTriggers.listener.EventListener;
import com.reil.bukkit.rTriggers.listener.SetupListener;
import com.reil.bukkit.rTriggers.persistence.TriggerLimit;
import com.reil.bukkit.rTriggers.timers.TimeKeeper;
import com.reil.bukkit.rTriggers.timers.rTriggersTimer;


public class rTriggers extends JavaPlugin {
	public static rTriggers plugin;
	
	public Logger log = Logger.getLogger("Minecraft");
	public rPropertiesFile Messages;
	
	private boolean registered;
	public boolean useRegister;
	public boolean useiNetLocator;
	
	private SetupListener serverListener = new SetupListener(this);
	private Listener playerListener = new EventListener(this);
	private CommandListener commandListener = new CommandListener(this);
	
	public Formatter formatter;
	public Dispatcher dispatcher;

	public CraftIRC CraftIRCPlugin;
	public PermissionsAdaptor permAdaptor;
	public Plugin ServerEventsPlugin;
    
	public TimeKeeper clock;
	
	public static final String commaSplit = "[ \t]*,[ \t]*";
	public static final String colonSplit = "[ \t]*:[ \t]*";

	@Override
	public void onEnable(){
		rTriggers.plugin = this;
		
		getDataFolder().mkdir();
        Messages = new rPropertiesFile(getDataFolder().getPath() + "/rTriggers.properties");
        clock = new TimeKeeper(this, getServer().getScheduler(), 0);
        
        
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
        HashMap <String, HashSet<String>> optionsMap = new HashMap<String,HashSet<String>>();
        List<String> permissionTriggerers = new LinkedList<String>();
		try {
			largestDelay = processOptions(Messages.load(), optionsMap);
			for (String key : Messages.getKeys()){
				if (key.startsWith("<<hasperm|") || key.startsWith("not|<<hasperm|")){
					permissionTriggerers.add(key.substring(key.lastIndexOf("|") + 1,key.length() - 2));
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "[rTriggers]: Exception while loading properties file.", e);
		}
		generateTimers(Messages);
		
		dispatcher = new Dispatcher(optionsMap, permissionTriggerers);
		
		
		// Set up the database if needed (only needed for delays)
		if (optionsMap.containsKey("delay")) {
	        try {
	            getDatabase().find(TriggerLimit.class).findRowCount();
	        } catch (PersistenceException ex) {
	            System.out.println("[rTriggers] Setting up persistence...");
	        }
			log.info("[rTriggers] Cleaned " + dispatcher.limitTracker.cleanEntriesOlderThan(largestDelay) + " entries from delay persistence table");
		}
		
		// Special settings line: timezone
		TimeZone timeZone;
		if (Messages.keyExists("s:timezone")){
			timeZone = new SimpleTimeZone(Messages.getInt("s:timezone")*3600000, "Server Time");
		} else timeZone = TimeZone.getDefault();
		
		// Do onload events for everything that might have loaded before rTriggers
		serverListener.checkAlreadyLoaded(getServer().getPluginManager());
		
		formatter = new Formatter(this, timeZone);
		
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


	/**
	 * Goes through each message in messages[] and registers events that it sees in each.
	 * @param messages
	 */
	public int processOptions(String[] messages, Map <String, HashSet<String>> optionsMap){
		int largestLimit = 0;
		if (registered) return 0;
		else registered = true;
		
		boolean [] flag = new boolean[9];
		Arrays.fill(flag, false);
		
		for(String message : messages){
			String [] split = message.split(Dispatcher.colonSplit, 3);
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
					commandListener.addCommand(option);
				} else if (option.startsWith("onconsole|")) {
					commandListener.addConsoleCommand(option);
				}
				if(!optionsMap.containsKey(option)) {
					optionsMap.put(option, new HashSet<String>());
				}
				optionsMap.get(option).add(message);
			}
		}
		return largestLimit * 1000;
	}

	/**
	 *  Checks to see if any rTriggers-supported plugins have already been loaded.
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
								new rTriggersTimer(message),
								waitTime, waitTime);
					}
				}
			} catch (NumberFormatException e){
				log.log(Level.WARNING, "[rTriggers] Invalid number string:" + key);
			}
		}
		if (messages.keyExists("<<timer>>")) log.log(Level.WARNING, "[rTriggers] Using old timer format! Please update to new version.");
	}
	
	public EbeanServer getDatabase() {
		EbeanServer database = Ebean.getServer("rTriggers");
		
		if(database == null){
			ServerConfig config = new ServerConfig();
			config.setName("rTriggers");
			config.setDefaultServer(true);
			config.addClass(TriggerLimit.class);
			
			database = EbeanServerFactory.create(config);
		}
		
		return database;
	}
}