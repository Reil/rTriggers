package com.reil.bukkit.rTriggers.listener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.Listener;

import com.reil.bukkit.rTriggers.rTriggers;

public class CommandListener implements Listener {
	private Map <String, List<CommandData>> commandMap = new HashMap<String, List<CommandData>>();
	private Map <String, List<CommandData>> consoleMap = new HashMap<String, List<CommandData>>();
	private rTriggers plugin;
	
	public CommandListener(rTriggers plugin){
		this.plugin = plugin;
	}
	
	public void addCommand(String optionString) {
		CommandData data = new CommandData(optionString);
		updateMap(commandMap, data);
	}
	
	public void addConsoleCommand(String optionString) {
		CommandData data = new CommandData(optionString);
		updateMap(consoleMap, data);
	}
	
	public void clearMaps(){
		commandMap = new HashMap<String, List<CommandData>>();
		consoleMap = new HashMap<String, List<CommandData>>();
	}
	
	private void updateMap(Map<String, List<CommandData>> updateMe, CommandData withMe) {
		if(!updateMe.containsKey(withMe.command)){
			updateMe.put(withMe.command, new LinkedList<CommandData>());
		}
		updateMe.get(withMe.command).add(withMe);
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		
		String [] split = event.getMessage().split(" ");
		String command = split[0].toLowerCase();
		int numParams = split.length - 1;
		
		if(!commandMap.containsKey(command)) return;
		
		List<String> replaceThese = new LinkedList<String>();
		List<String> withThese    = new LinkedList<String>();
		buildParameterLists(split, replaceThese, withThese);
		String [] replaceTheseArray = replaceThese.toArray(new String[replaceThese.size()]);
		String [] withTheseArray = withThese.toArray(new String[withThese.size()]);

        for(CommandData data : commandMap.get(command)) {
        	if(data.match(numParams)) {
        		if(plugin.dispatcher.triggerMessages(player, data.options, replaceTheseArray, withTheseArray)
        				&& data.override) {
        			event.setCancelled(true);
        		}
        	}
        }
		
		return; 
	}
	
	@EventHandler
	public void onServerCommand(ServerCommandEvent event){
		String [] split = event.getCommand().split(" ");
		String command = split[0].toLowerCase();
		
		int numParams = split.length - 1;
		
		if(!consoleMap.containsKey(command)) return;
		
		List<String> replaceThese = new LinkedList<String>();
		List<String> withThese    = new LinkedList<String>();
		buildParameterLists(split, replaceThese, withThese);
		String [] replaceTheseArray = replaceThese.toArray(new String[replaceThese.size()]);
		String [] withTheseArray = withThese.toArray(new String[withThese.size()]);
        
        for(CommandData data : commandMap.get(command)){
        	if(data.match(numParams))
        	{
        		if(plugin.dispatcher.triggerMessages(data.options, replaceTheseArray, withTheseArray)
        				&& data.override){
        			// We can't override console commands, so instead,
        			// we can set this to a console command that we CAN
        			// intercept.
        			event.setCommand("rTriggers");
        		}
        	}
        }
		
		return; 
	}

	private void buildParameterLists(String[] split, List<String> replaceThese,
			List<String> withThese) {
		/* Build parameter list */
		StringBuilder params = new StringBuilder();
		StringBuilder reverseParams = new StringBuilder();
		String prefix = ""; 
		int max = split.length;
		for(int i = 1; i < max; i++){
			params.append(prefix + split[i]);
			reverseParams.insert(0, split[max - i] + prefix);
			prefix = " ";
			
			replaceThese.add("<<param" + i + ">>");
			withThese.add(split[i]);
			
			replaceThese.add("<<param" + i + "->>");
			withThese.add(params.toString());

			replaceThese.add("<<param" + (max - i) + "\\+>>");
			withThese.add(reverseParams.toString());
		}
		replaceThese.add("<<params>>");
		withThese.add(params.toString());
	}
	
	private class CommandData {
		boolean greaterThan;
		boolean lessThan;
		boolean equalTo;
		int numParams;
		
		boolean override;
		
		String options;
		String command;
		
		public CommandData(String optionString){			
			options = optionString;
			
			// Extract command
			int firstBar = optionString.indexOf('|') + 1;
			int secondBar = optionString.indexOf('|', firstBar);
			if (secondBar != -1) {
				command = optionString.substring(firstBar, secondBar);
			}
			else {
				command = optionString.substring(firstBar);
			}
			
			if(options.contains("override")){
				override = true;
			} else {
				override = false;
			}
			
			if(options.endsWith("+")) {
				greaterThan = true;
				lessThan = false;
				equalTo = false;
			} else if(options.endsWith("-")) {
				lessThan = true;
				greaterThan = false;
				equalTo = false;
			} else {
				lessThan = false;
				greaterThan = false;
				equalTo = true;
			}
			
			try {
				int lastIndex = options.length();
				if(options.endsWith("+") || options.endsWith("-")){
					lastIndex--;
				}
				String paramString = options.substring(options.lastIndexOf('|') + 1, lastIndex);
				numParams = Integer.parseInt(paramString);
			} catch (NumberFormatException e) {
				// Either poor format, or number doesn't exist
				numParams = -1;
			}
		}
		
		boolean match(int number){
			return (lessThan    && (number <= numParams)) ||
				   (greaterThan && (number >= numParams)) ||
				   (equalTo     && (number == numParams)) ||
					numParams == -1;
		}
	}
}
