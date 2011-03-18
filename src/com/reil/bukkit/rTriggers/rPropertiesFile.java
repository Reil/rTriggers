package com.reil.bukkit.rTriggers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import com.reil.bukkit.rParser.rParser;

public class rPropertiesFile {
	HashMap<String,ArrayList<String>> Properties = new HashMap<String,ArrayList<String>>(); 
	String fileName;
	Logger log = Logger.getLogger("Minecraft");
	File file;

	/**
     * Creates or opens a properties file using specified filename
     * 
     * @param fileName
     */
    public rPropertiesFile(String fileName) {
        this.fileName = fileName;
        file = new File(fileName);

        if (file.exists()) {
            try {
                load();
            } catch (IOException ex) {
                log.severe("[PropertiesFile] Unable to load " + fileName + "!");
            }
        } else {
            try {
            	file.createNewFile();
            	Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
            	Date timestamp = new Date();
            	writer.write("# Properties file generated on " + timestamp.toString());
            	writer.close();
            } catch (IOException ex) {
            	log.severe("[rPropertiesFile] Unable to create file " + fileName + "!");
            }
        }
    }
	
	String[] load() throws IOException {
		/* Go through, line by line. 
		 * If the line starts with # or !, then save the line in list
		 * If the line has an assignment, put the name here. */
		Properties.clear();
		ArrayList<String> messages = new ArrayList<String>();
		BufferedReader reader;
	    reader = new BufferedReader(new FileReader(fileName));
	    String line;
	    while ((line = reader.readLine()) != null) {
	    	if (line.startsWith("#")|| line.isEmpty() || line.startsWith("\n") || line.startsWith("\r")) {
	    		
	    	}
	    	else {
	    		/* TODO: Error checking */
	    		String [] split = line.split("=");
	    		if (split.length >= 2){
	        		String PropertySide = split[0];
	        		String Value = rParser.combineSplit(1, split, "=");
	        		for (String Property : PropertySide.split(",")) {
		        		if (Properties.containsKey(Property)){
		        			Properties.get(Property).add(Value);
		        		}
		        		else {
		        			ArrayList<String> newList = new ArrayList<String>();
		        			newList.add(Value);
		        			Properties.put(Property, newList);
		        		}
	        		}
	        		messages.add(Value);
	    		}
	    	}
	    }
	    reader.close();
	    return messages.toArray(new String[messages.size()]);
	}

	void save(){
		
	}

	boolean getBoolean(java.lang.String key) {
		return true; 
	}
	boolean	getBoolean(java.lang.String key, boolean value) {
		return true;
	}
	int	getInt(java.lang.String key){
		return 0;
	}
	int	getInt(java.lang.String key, int value){
		return 0;
	}
	long getLong(java.lang.String key) {
		return 0;
	}
	long getLong(java.lang.String key, long value){
		return 0;
	}
	
	String getString(java.lang.String key) {
		ArrayList<String> arrayList = Properties.get(key);
		return arrayList.get(0);
	}
	
	String getString(java.lang.String key, java.lang.String value) {
		if (Properties.containsKey(key)){
			ArrayList<String> arrayList = Properties.get(key);
			return arrayList.get(0);
		}
		else {
			setString(key, value); 
		}
		return value;
	}
	
	
	String [] getStrings(String key) {
		if (Properties.containsKey(key)) {
			ArrayList <String> rt = Properties.get(key);
			return rt.toArray(new String[rt.size()]);
		} else return null;
	}
	
	String [] getStrings(String [] keys){
		ArrayList <String> returnMe = new ArrayList<String>();
		for (String key : keys) {
			if (Properties.containsKey(key)) {
				ArrayList <String> rt = Properties.get(key);
				returnMe.addAll(rt);
			}
		}
		String[] returnArray = new String[returnMe.size()];
		return returnMe.toArray(returnArray);
	}
	
	String [] getKeys() {
		Set<String> keys = Properties.keySet();
		String [] keyArray = new String[keys.size()];
		return keys.toArray(keyArray);
	}
	
	boolean	keyExists(java.lang.String key) {
		return Properties.containsKey(key);
	}
	void setBoolean(java.lang.String key, boolean value) {
		
	}
	void setInt(java.lang.String key, int value) {
		
	}
	void setLong(java.lang.String key, long value) {
		
	}
	void setString(java.lang.String key, java.lang.String value) {
		
	}
}
