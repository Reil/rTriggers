package com.reil.bukkit.rTriggers;
import java.util.ArrayList;


public class MessageParser {
	protected static final int lineLength = 312;
	
	public static String parseMessage(String message, String [] replace, String[] with){
		String parsed = message;
		for(int i = 0; i < replace.length; i++) {
			parsed = parsed.replaceAll(replace[i], with[i]);
		}
		String parsed2 = new String();
		for (String toParse : parsed.split("\n")) {
			parsed2 += lastColor(parsed2) + combineSplit(0, wordWrap(toParse), "\n") + "\n";
		}
		return parsed2;
	}
	public static String lastColor(String findColor) {
		int i = findColor.lastIndexOf('§');
		if (i != -1 && i != findColor.length() - 1)
			return "§" + findColor.charAt(i+1);		
		else return "§f";
	}
	public static String combineSplit(int beginHere, String [] split, String seperator){
		String combined = new String(split[beginHere]);
		for (int i = beginHere + 1; i < split.length; i++){
			combined = combined + seperator + split[i];
		}
		return combined;
	}
	
	
	public static String[] wordWrap(String msg){
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
        	ArrayList<String> words = new ArrayList<String>();

    		//Loop through the words finding their length and increasing
    		//j, the end point for the sub string
    		while(!split.isEmpty() && split.get(0) != null && len <= lineLength)
    		{
    			int wordLength = msgLength(split.get(0)) + 4;
    			
    			//If a word is too long for a line
    			if(wordLength > lineLength)
    			{
        			String[] tempArray = wordCut(len, split.remove(0));
    				words.add(tempArray[0]);
    				
        			split.add(tempArray[1]);
    			}

    			//If the word is not too long to fit
    			len += wordLength;
    			if( len < lineLength)
    				words.add(split.remove(0));
    		}
    		//Merge them and add them to the output array.
    		out.add( combineSplit(0,
    				words.toArray(new String[words.size()]), " ") + " " );
    	}
    	//Convert to an array and return
    	return out.toArray(new String[out.size()]);
    }
	
	//=====================================================================
	//Function:	msgLength
	//Input:	String str: The string to find the length of
	//Output:	int: The length on the screen of a string
	//Use:		Finds the length on the screen of a string. Ignores colors.
	//=====================================================================
	 public static int msgLength(String str){
		int length = 0;
		//Loop through all the characters, skipping any color characters
		//and their following color codes
		for(int x = 0; x<str.length(); x++)
		{
			if(str.charAt(x) == '§' /*|| str.charAt(x) == Colors.White.charAt(0)*/)
			{
				if(colorChange(str.charAt(x + 1)) != null)
				{
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
	//Function:	wordCut
	//Input:	String str: The string to find the length of
	//Output:	String[]: The cut up word
	//Use:		Cuts apart a word that is too long to fit on one line
	//=====================================================================
	 private static String[] wordCut(int lengthBefore, String str){
		int length = lengthBefore;
		//Loop through all the characters, skipping any color characters
		//and their following color codes
		String[] output = new String[2];
		int x = 0;
		while(length < lineLength && x < str.length())
		{
			int len = charLength(str.charAt(x));
			if( len > 0)
				length += len;
			else
				x++;
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
    
  //=====================================================================
	//Function:	colorChange
	//Input:	char colour: The color code to find the color for
	//Output:	String: The color that the code identified 
	//Use:		Finds a color giving a color code
	//=====================================================================
	public static String colorChange(char colour)
	{
		if ((colour >= '0' && colour <= '9') ||
				(colour >= 'a' && colour <= 'f')||
				(colour >= 'A' && colour <= 'F')){
			return "b";
		}
		else return null;
		/*String color = "";
		switch(colour)
		{
			case '0':
				color = Colors.Black;
				break;
			case '1':
				color = Colors.Navy;
				break;
			case '2':
				color = Colors.Green;
				break;
			case '3':
				color = Colors.Blue;
				break;
			case '4':
				color = Colors.Red;
				break;
			case '5':
				color = Colors.Purple;
				break;
			case '6':
				color = Colors.Gold;
					break;
			case '7':
				color = Colors.LightGray;
				break;
			case '8':
				color = Colors.Gray;
				break;
			case '9':
				color = Colors.DarkPurple;
				break;
			case 'a':
				color = Colors.LightGreen;
				break;
			case 'b':
				color = Colors.LightBlue;
				break;
			case 'c':
				color = Colors.Rose;
				break;
			case 'd':
				color = Colors.LightPurple;
				break;
			case 'e':
				color = Colors.Yellow;
				break;
			case 'f':
				color = Colors.White;
				break;
			case 'A':
				color = Colors.LightGreen;
				break;
			case 'B':
				color = Colors.LightBlue;
				break;
			case 'C':
				color = Colors.Rose;
				break;
			case 'D':
				color = Colors.LightPurple;
				break;
			case 'E':
				color = Colors.Yellow;
				break;
			case 'F':
				color = Colors.White;
				break;
			case 'R':
				color = "~";
				break;
			case 'r':
				color = "~";
				break;
			default:
				color = null;
				break;
		}
		return color;*/
	}
}
