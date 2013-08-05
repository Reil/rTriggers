package com.reil.bukkit.rTriggers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import net.sf.javainetlocator.InetAddressLocator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	
	public static String replaceWords(String message, String [] replace, String[] with){
		String parsed = message;
		for(int i = 0; i < replace.length && i <with.length; i++) {
			parsed = parsed.replaceAll(replace[i], with[i]);
		}
		return parsed;
	}
	
	protected static final int lineLength = 312;
	
	public static String parseMessage(String message, String [] replace, String[] with){
		String parsed = replaceWords(message, replace, with);
		StringBuilder output = new StringBuilder();
		ArrayList<String> outputArray = new ArrayList<String>();
		for (String toParse : parsed.split("\n")) {
			for(String add : wordWrap(toParse))
				outputArray.add(add);
		}
		for(String addMe : outputArray){
			output.append(lastColor(output.toString()) + addMe + "\n");
		}
		return output.toString();
	}
	
	public static String[] wordWrap(String msg){
		return wordWrap(msg, "", lineLength);
	}
	
	public static String lastColor(String findColor) {
		int i = findColor.lastIndexOf('§'); 
		if (i != -1 && i != findColor.length() - 1)
			return "§" + findColor.charAt(i+1);		
		else return "";
	}
	
	public static String[] wordWrap(String msg, String prefix, int lineLength){
    	//Split each word apart
    	ArrayList<String> split = new ArrayList<String>();
    	for(String in : msg.split(" "))
			split.add(in);
    	
    	//Create an arraylist for the output
    	ArrayList<String> out = new ArrayList<String>();
    	//While i is less than the length of the array of words
    	while(!split.isEmpty()){
    		int len = 0;
        	
        	//Create an arraylist to hold individual words
        	List<String> words = new LinkedList<String>();

    		//Loop through the words finding their length and increasing
    		//j, the end point for the sub string
    		while(!split.isEmpty() && split.get(0) != null && len <= lineLength)
    		{
    			int wordLength = msgLength(split.get(0)) + 4;
    			
    			//If a word is too long for a line
    			if(wordLength > lineLength)
    			{
        			String[] tempArray = wordCut(len, split.remove(0), lineLength);
    				words.add(tempArray[0]);
    				
        			split.add(tempArray[1]);
    			}

    			//If the word is not too long to fit
    			len += wordLength;
    			if( len < lineLength)
    				words.add(split.remove(0));
    		}
    		//Merge them and add them to the output array.
    		String lastColor = "";
    		if (!out.isEmpty()){
    			 lastColor = lastColor(out.get(out.size() - 1));
    		}
    		out.add(lastColor + 
    				combineSplit(0,	words.toArray(new String[words.size()]), " ") + " " );
    	}
    	
    	//Convert to an array and return
    	return out.toArray(new String[out.size()]);
    }
	
	//=====================================================================
	//Function:	msgLength
	//Input:	String str: The string to find the length of
	//Output:	int: The length on the screen of a string
	//Use:		Finds the length on the screen of a string. Ignores ChatColor.
	//=====================================================================
	 public static int msgLength(String str){
		int length = 0;
		//Loop through all the characters, skipping any color characters
		//and their following color codes
		for(int x = 0; x<str.length(); x++) {
			if(str.charAt(x) == '§') {
				if (x+1 != str.length() && colorChange(str.charAt(x + 1)) != null) {
					x++;
					continue;
				}
			}
			int len = charLength(str.charAt(x));
			length += len;
		}
		return length;
    }
	 
	//=====================================================================
	//Function:	colorChange
	//Input:	char colour: The color code to find the color for
	//Output:	String: The color that the code identified 
	//Use:		Finds a color giving a color code
	//=====================================================================
	public static String colorChange(char colour)
	{
		ChatColor color;
		switch(colour)
		{
			case '0':
				color = ChatColor.BLACK;
				break;
			case '1':
				color = ChatColor.DARK_BLUE;
				break;
			case '2':
				color = ChatColor.DARK_GREEN;
				break;
			case '3':
				color = ChatColor.DARK_AQUA;
				break;
			case '4':
				color = ChatColor.DARK_RED;
				break;
			case '5':
				color = ChatColor.DARK_PURPLE;
				break;
			case '6':
				color = ChatColor.GOLD;
				break;
			case '7':
				color = ChatColor.GRAY;
				break;
			case '8':
				color = ChatColor.DARK_GRAY;
				break;
			case '9':
				color = ChatColor.BLUE;
				break;
			case 'a':
				color = ChatColor.GREEN;
				break;
			case 'b':
				color = ChatColor.AQUA;
				break;
			case 'c':
				color = ChatColor.RED;
				break;
			case 'd':
				color = ChatColor.LIGHT_PURPLE;
				break;
			case 'e':
				color = ChatColor.YELLOW;
				break;
			case 'f':
				color = ChatColor.WHITE;
				break;
			case 'A':
				color = ChatColor.GREEN;
				break;
			case 'B':
				color = ChatColor.AQUA;
				break;
			case 'C':
				color = ChatColor.RED;
				break;
			case 'D':
				color = ChatColor.LIGHT_PURPLE;
				break;
			case 'E':
				color = ChatColor.YELLOW;
				break;
			case 'F':
				color = ChatColor.WHITE;
				break;
			default:
				return null;
		}
		return color.toString();
	}
	
	//=====================================================================
	//Function:	charLength
	//Input:	char x: The character to find the length of.
	//Output:	int: The length of the character
	//Use:		Finds the visual length of the character on the screen.
	//=====================================================================
    private static int charLength(char x)
    {
    	if("i.:,;|!".indexOf(x) != -1)
			return 2;
		else if("l'".indexOf(x) != -1)
			return 3;
		else if("tI[]".indexOf(x) != -1)
			return 4;
		else if("fk{}<>\"*()".indexOf(x) != -1)
			return 5;
		else if("abcdeghjmnopqrsuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ1234567890\\/#?$%-=_+&^".indexOf(x) != -1)
			return 6;
		else if("@~".indexOf(x) != -1)
			return 7;
		else if(x==' ')
			return 4;
		else
			return -1;
    }
    
	public static String combineSplit(int beginHere, String [] split, String seperator){
		StringBuilder combined = new StringBuilder(split[beginHere]);
		if(beginHere + 1 < split.length){
			for (int i = beginHere + 1; i < split.length; i++){
				combined.append(seperator + split[i]);
			}
		}
		return combined.toString();
	}
	
	//=====================================================================
	//Function:	wordCut
	//Input:	String str: The string to find the length of
	//Output:	String[]: The cut up word
	//Use:		Cuts apart a word that is too long to fit on one line
	//=====================================================================
	 private static String[] wordCut(int lengthBefore, String str, int lineLength){
		int length = lengthBefore;
		//Loop through all the characters, skipping any color characters
		//and their following color codes
		String[] output = new String[2];
		int x = 0;
		while(length < lineLength && x < str.length())
		{
			int len = charLength(str.charAt(x));
			if( len > 0) length += len;
			else x++;
			x++;
		}
		if(x > str.length())
			x = str.length();
		//Add the substring to the output after cutting it
		output[0] = str.substring(0, x);
		//Add the last of the string to the output.
		output[1] = str.substring(x);
		return output;
    }
	 
}
