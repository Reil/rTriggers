package com.reil.bukkit.rTriggers;
import java.io.*;
import java.util.*;
import java.util.logging.*;

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
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
	    String tempLine;
	    
	    //Cycle through complete contents of the file.	 
	    while ((tempLine = reader.readLine()) != null) {
	    	/*****************
	    	* Credit for original line-continuation implementation goes to ioScream!*/
	    	// Check for multiple lines with <<;>> and recreate them as one line.
            StringBuilder concatMe = new StringBuilder(tempLine);
    		while (concatMe.toString().endsWith("$")){
                //Skip next element because it's been merged
    			concatMe.deleteCharAt(concatMe.length() - 1);
    			if ((tempLine = reader.readLine()) != null)
	    			concatMe.append(tempLine);
    		}
    		String line = concatMe.toString();
    		// Line is now built, read it in as usual.
    		
	    	if (line.startsWith("#")|| line.isEmpty() || line.startsWith("\n") || line.startsWith("\r"))
	    		continue;
	    	
    		String [] split = line.split("[ \t]*?=[ \t]*?", 2);
    		if (!(split.length == 2)) continue;
    		
    		String propertyKeys = split[0];
    		String propertyValue = split[1];
    		
    		for (String key : propertyKeys.split(",")) {
    			if (key.startsWith("<<") && !key.startsWith("<<list")) key = key.toLowerCase();
        		
    			if (Properties.containsKey(key))
        			Properties.get(key).add(propertyValue);
        		else {
        			ArrayList<String> newList = new ArrayList<String>();
        			newList.add(propertyValue);
        			Properties.put(key, newList);
        		}
    		}
    		messages.add(propertyValue);
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
		return Integer.parseInt(Properties.get(key).get(0).trim());
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
