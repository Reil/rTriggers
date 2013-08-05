package com.reil.bukkit.rTriggers;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import net.sf.javainetlocator.InetAddressLocator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nijikokun.register.payment.Methods;
import com.reil.bukkit.rParser.rParser;

public class Formatter {
	/*
	 * Start: Functions for formatting messages
	 */
	
	/* Takes care of replacements that don't vary per player. */
	
	private static TimeZone timeZone;
	
	public Map <String, Integer> listTracker;
	public Random RNG;
	
	
	public Formatter(rTriggers plugin, TimeZone timeZone){
		listTracker = new HashMap<String,Integer>();
		RNG = new Random();
		Formatter.timeZone = timeZone;
	}
	
	public static String stdReplace(String message) {
		Calendar time = Calendar.getInstance(timeZone);
		String minute = String.format("%tM", time);
		String hour   = Integer.toString(time.get(Calendar.HOUR));
		String hour24 = String.format("%tH", time);
		String [] replace = {"(?<!\\\\)@", "(?<!\\\\)&", "<<color>>","\\\\&", "\\\\@", "<<time>>"          ,"<<time\\|24>>"        ,"<<hour>>", "<<minute>>", "<<player-count>>"};
		String [] with    = {"\n§f"      , "§"         , "§"        ,"&"    , "@"    , hour + ":" + minute,hour24 + ":" + minute  , hour     , minute      , Integer.toString(Bukkit.getServer().getOnlinePlayers().length)};
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
			String [] messageList = rTriggers.plugin.Messages.getStrings("<<list|" + optionSplit[0] + ">>");
			
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
			
			for (Player getName : rTriggers.plugin.getServer().getOnlinePlayers()){
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
		if (rTriggers.plugin.useRegister && Methods.getMethod() != null && Methods.getMethod().hasAccount(player.getName()))
			balance = Methods.getMethod().getAccount(player.getName()).balance();
		
		// Get ip and locale tags
		InetSocketAddress IP = player.getAddress();
		String country = "";
		String locale = "";
		String IPString;
		
		try {
			if (rTriggers.plugin.useiNetLocator) {
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
