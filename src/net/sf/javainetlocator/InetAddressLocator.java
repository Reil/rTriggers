/*
 * @(#)InetAddressLocator.java 2.20, 22/10/2004
 */


package net.sf.javainetlocator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * <p>This class discovers geographical location from IP addresses.</p>
 *
 * <p>
 * Copyright 2002/03 Nigel Wetters Gourlay, Paul Hastings. All rights reserved.
 * </p>
 * 
 * <p>This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 
 * of the License, or(at your option) any later version.</p>
 * 
 * <p>This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.</p>
 * 
 * <p>You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA</p>
 * 
 * @author  Nigel Wetters Gourlay, Paul Hastings
 * @version 2.20, 02/06/05
 * @see java.net.InetAddress
 * @see java.util.Locale
 */
public class InetAddressLocator {
    // Determines if a de-serialized file is compatible with this class.
    private final static long serialVersionUID = 3833768499L;

    // Some useful constants
    private final static String thisClass   = "net.sf.javainetlocator.InetAddressLocator";
    private final static String thisPath    = "net/sf/javainetlocator/";
    private final static String locPropPath = thisPath + "locale.props";
    private final static String ipDbPath    = thisPath + "ip";
    private final static String ccDbPath    = thisPath + "cc";

    private final byte[]   ip_db;
    private final Locale[] cc_db;
    private final static InetAddressLocator me; // singleton

    static {
	/*
	 * Singleton construction.
	 * We use a singleton to reduce memory and increase performace 
	 * (the IP and country code databases are read only once from
	 * disk). Note that no information is written to member 
	 * variables after the constructor has been called, so (I hope)
	 * there are no concurrency/synchronization problems here.
	 */
	try {
	    me = new InetAddressLocator();
	} catch (InetAddressLocatorException e) {
	    throw new java.lang.RuntimeException("error during class loading",e);
	}
    }
    
    /**
     * The constructor must not to be called outside of this class.
     * It initializes IP and country-code databases.
     * @throws InetAddressLocatorException if databases could not be 
     * initialized.
     */
    @SuppressWarnings("rawtypes")
	private InetAddressLocator() throws InetAddressLocatorException {
	Map locales = load_locales();
	ip_db = load_ip_database();
	cc_db = load_cc_database(locales);
    }
    
    /**
     * Reads the locale.props properties file and populates the 
     * locale_map
     * @throws InetAddressLocatorException if problem reading 
     * locale.props or badly formated property file.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map load_locales() throws InetAddressLocatorException {
	Map locale_map = new java.util.HashMap();
	Properties countryLangPairs = new Properties();
	try {
	    ClassLoader loader = Class.forName(thisClass).getClassLoader();
	    countryLangPairs.load(loader.getResourceAsStream(locPropPath));
	} catch (Exception e) {
	    throw new InetAddressLocatorException("couldn't read locale.props file",e);
	}
	for (Enumeration e = countryLangPairs.propertyNames(); e.hasMoreElements(); ) {
	    String country = (String) e.nextElement();
	    if (country.length() != 2) {
		throw new InetAddressLocatorException("bad country name in locale.props:" + country);
	    }
	    String language = countryLangPairs.getProperty(country, "");
	    if (language.length() != 2) {
		throw new InetAddressLocatorException("bad language naname in locale.props: " + language);
	    }
	    locale_map.put(country, new Locale(language, country));
	}
	
	// Address not found in database
	locale_map.put("--", new Locale("", ""));
	
	return locale_map;
    }
    
    /**
     * Loads the IP database from ip.gif
     * @return IP database
     * @throws InetAddressLocatorException if the IP database could not
     * be read
     */
    private static byte[] load_ip_database()
	throws InetAddressLocatorException {
	try {
	    return getResourceAsByteArray(ipDbPath);
	} catch (Exception e) {
	    throw new InetAddressLocatorException("couldn't read IP database",e);
	}
    }
    
    /**
     * Loads the country-code database from cc.gif
     * @return country code database
     * @throws InetAddressLocatorException if the country-code database
     * could not be read
     */
    private static Locale[] load_cc_database(@SuppressWarnings("rawtypes") Map locale_map)
	throws InetAddressLocatorException {
	Locale[] db = new Locale[256];
	byte[] raw_file;
	
	try {
	    raw_file = getResourceAsByteArray(ccDbPath);
	} catch (Exception e) {
	    throw new InetAddressLocatorException("couldn't read country-code database",e);
	}
	
	for (int i = 0; i < raw_file.length; i = i + 3) {
	    int id = complementToUnsigned(raw_file[i]);
	    String cc = "" +
		(char) complementToUnsigned(raw_file[i + 1]) +
		(char) complementToUnsigned(raw_file[i + 2]);
	    Locale country = (Locale) locale_map.get(cc);
	    if (country == null) {
		throw new InetAddressLocatorException("country code must appear in locale.props (but doesn't): " + cc);
	    } else {
		db[id] = country;
	    }
	}
	return db;
    }
    
    /**
     * Helper function to read a file into a byte array.
     * @param path the path to the file
     * @return the contents of the file
     * @throws IOException if there was an error reading the file
     */
    private static byte[] getResourceAsByteArray(String path)
	throws IOException, NoClassDefFoundError, ClassNotFoundException {
	ClassLoader loader = Class.forName(thisClass).getClassLoader();
	InputStream is = new BufferedInputStream(loader.getResourceAsStream(path));
	
	/*
	 * default size of byte array set to 256
	 * (large enough for the country-code database,
	 * but not large enough for the IP database).
	 */
	byte[] byte_arr = new byte[256];
	
	/*
	 * keep track of the index of the byte array:
	 * we need to know whether we might insert to
	 * an index outside the bounds of the array
	 * (i.e. whether we are about to generate an 
	 * ArrayIndexOutOfBoundsException).
	 */
	int arr_cursor = 0;
	
	/*
	 * although we're reading a byte at a time, this is
	 * a buffered input stream, which handles reading the
	 * file in chunks - thereby increasing throughput
	 */
	int n;
	while ((n = is.read()) != -1) {
	    
	    /*
	     * double the size of byte_arr if we're about
	     * to generate ArrayIndexOutOfBoundsException
	     */
	    if (arr_cursor >= byte_arr.length) {
		byte[] tmp = new byte[byte_arr.length * 2];
		System.arraycopy(byte_arr, 0, tmp, 0, byte_arr.length);
		byte_arr = tmp;
	    }
	    
	    /*
	     * read() returns an integer representation of
	     * the read byte [the final byte of the integer has an 
	     * identical bit pattern, but has a range (0 to 255) 
	     * as opposed to the range of a byte (-128 to 127)].
	     * Thus, we use the unsignedToComplement method to 
	     * convert back to Java's native byte format.
	     */
	    byte_arr[arr_cursor] = unsignedToComplement(n);
	    arr_cursor++;
	}
	is.close();
	
	/*
	 * when we increase the size of byte_arr, we probably
	 * added too many empty elements. This next loop creates
	 * a new byte array of the correct size, which will be 
	 * returned from the method. Thus, the length of the
	 * returned array will be identical to the length of the 
	 * file.
	 */
	byte[] tmp = new byte[arr_cursor];
	System.arraycopy(byte_arr, 0, tmp, 0, arr_cursor);
	byte_arr = tmp;
	
	return byte_arr;
    }
    
    /**
     * <p>When passed an {@link String} containing a hostname, will
     * return a {@link Locale} object corresponding to the country where
     * the host was allocated.</p>
     *
     * <p>The country of the Locale (retrieved by 
     * {@link Locale#getCountry()}) is set to the 
     * <a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">international 
     * two-letter code</a> for the country where the
     * Internet address was allocated (e.g. NZ for New Zealand).</p>
     * 
     * <p>If the Internet address cannot be found within the database, 
     * the country and language of the returned Locale are set to empty 
     * Strings.</p>
     *
     * <p>Three country values can be returned that do not exist within
     * the international standard (ISO 3166). These are EU (for a 
     * nonspecific European address), AP (for a nonspecific Asia-Pacific
     * address) and ** (an Internet address 
     * <a href="http://www.ietf.org/rfc/rfc3330.txt">reserved for 
     * private use</a>, for example on a corporate network not available
     * from the public Internet).</p>
     * 
     * <p>The language of the returned Locale (retrieved by 
     * {@link Locale#getLanguage()} is set to the 
     * <a href="http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt">international
     * two-letter code</a> for the official language of the country 
     * where the Internet address was allocated.</p>
     *
     * <p>Where a country has more than one official language, the
     * language is set to that which has the majority of native 
     * speakers. For example, the language for Canada is set to English
     * (en) rather than French (fr).</p>
     *
     * <p>Nonspecific addresses (EU and AP), private Internet addresses 
     * (**), and addresses not found within the database, all return an
     * empty string for language.</p>
     *
     * @param host Internet address to be located
     * @return geographic region associated with the address
     * @throws InetAddressLocatorException if the database is corrupt
     */
    public static Locale getLocale(String host)
	throws InetAddressLocatorException {
	byte[] addr = addressToByteArray(host);
	if (addr != null){
	    return getLocale(addr);
	} else {
	    try {
		return getLocale(InetAddress.getByName(host));
	    } catch (java.net.UnknownHostException e){
		throw new InetAddressLocatorException(e.getMessage(),e);
	    }
	}
    }
    
    /**
     * <p>When passed an {@link InetAddress} object,
     * will return a {@link Locale} object corresponding to the country
     * where the address was allocated.</p>
     * 
     * <p>The country of the Locale (retrieved by 
     * {@link Locale#getCountry()}) is set to the 
     * <a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">international 
     * two-letter code</a> for the country where the
     * Internet address was allocated (e.g. NZ for New Zealand).</p>
     * 
     * <p>If the Internet address cannot be found within the database, 
     * the country and language of the returned Locale are set to empty 
     * Strings.</p>
     *
     * <p>Three country values can be returned that do not exist within
     * the international standard (ISO 3166). These are EU (for a 
     * nonspecific European address), AP (for a nonspecific Asia-Pacific
     * address) and ** (an Internet address 
     * <a href="http://www.ietf.org/rfc/rfc3330.txt">reserved for 
     * private use</a>, for example on a corporate network not available
     * from the public Internet).</p>
     * 
     * <p>The language of the returned Locale (retrieved by 
     * {@link Locale#getLanguage()} is set to the 
     * <a href="http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt">international
     * two-letter code</a> for the official language of the country 
     * where the Internet address was allocated.</p>
     *
     * <p>Where a country has more than one official language, the
     * language is set to that which has the majority of native 
     * speakers. For example, the language for Canada is set to English
     * (en) rather than French (fr).</p>
     *
     * <p>Nonspecific addresses (EU and AP), private Internet addresses 
     * (**), and addresses not found within the database, all return an
     * empty string for language.</p>
     *
     * @param address Internet address to be located
     * @return geographic region associated with the address
     * @throws InetAddressLocatorException if the database is corrupt
     */
    public static Locale getLocale(InetAddress address)
	throws InetAddressLocatorException {
	// the IP address, in byte-sized chunks
	return getLocale(address.getAddress());
    }
    
    /**
     * <p>When passed a byte array representing an IP address,
     * will return a {@link Locale} object corresponding to the country
     * where the address was allocated.</p>
     * 
     * <p>The country of the Locale (retrieved by 
     * {@link Locale#getCountry()}) is set to the 
     * <a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">international 
     * two-letter code</a> for the country where the
     * Internet address was allocated (e.g. NZ for New Zealand).</p>
     * 
     * <p>If the Internet address cannot be found within the database, 
     * the country and language of the returned Locale are set to empty 
     * Strings.</p>
     *
     * <p>Three country values can be returned that do not exist within
     * the international standard (ISO 3166). These are EU (for a 
     * nonspecific European address), AP (for a nonspecific Asia-Pacific
     * address) and ** (an Internet address 
     * <a href="http://www.ietf.org/rfc/rfc3330.txt">reserved for 
     * private use</a>, for example on a corporate network not available
     * from the public Internet).</p>
     * 
     * <p>The language of the returned Locale (retrieved by 
     * {@link Locale#getLanguage()} is set to the 
     * <a href="http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt">international
     * two-letter code</a> for the official language of the country 
     * where the Internet address was allocated.</p>
     *
     * <p>Where a country has more than one official language, the
     * language is set to that which has the majority of native 
     * speakers. For example, the language for Canada is set to English
     * (en) rather than French (fr).</p>
     *
     * <p>Nonspecific addresses (EU and AP), private Internet addresses 
     * (**), and addresses not found within the database, all return an
     * empty string for language.</p>
     *
     * @param ip Internet address to be located
     * @return geographic region associated with the address
     * @throws InetAddressLocatorException if the database is corrupt
     */
    public static Locale getLocale(byte[] ip)
	throws InetAddressLocatorException {
	// cursor position in me.ip_ranges
	int position = 4;
	
	// looping through each *byte* of the IP address
	for (byte i = 0; i < ip.length; i++) {
	    
	    // looping through each *bit* of each byte
	    for (byte j = 0; j < 8; j++) {
		
		/*
		 * Check whether the next record in the IP database 
		 * contains a terminal node (country code), or a binary
		 * branch (jump). A binary branch contains information
		 * on the distance to the next two nodes down the tree.
		 * This next statement should always return true, as 
		 * getLocale() returns if a country code is found (see
		 * lower).
		 */
		if (isJump(me.ip_db[position])) {
		    
		    /*
		     * length (in bytes) of database record holding jump
		     * distance (this is either one or three bytes).
		     * This default number assumes that the jump 
		     * distance is stored in one byte. Actually, only
		     * the lower six bits of the current database record
		     * can be used in a single-byte jump as the high 
		     * bytes are already being used (bit0 indicates 
		     * whether this is a jump or country code, bit1 
		     * indicates whether this is a one- or three- byte
		     * jump). Thus a single-byte jump must be between 
		     * 0 and 63.
		     */
		    int recordLength = 1;
		    boolean multiByte = false;
		    
		    // distance to next record measured from end of 
		    // this record
		    int jump = 0;
		    
		    // checks whether this record is actally three bytes
		    // in length
		    if (isMultiByteJump(me.ip_db[position])) {
			recordLength = 3;
			multiByte = true;
		    }
		    
		    /*
		     * Check whether the current bit of the IP address
		     * is set or unset. If the bit is unset (zero), the
		     * next nodes begin at the end of the current record
		     * (we only need to skip over the current record),
		     * whereas if the next is set (one), we need to 
		     * discover how far to jump.
		     */
		    if (bitIsSet(ip[i], j)) {
			
			/*
			 * The current bit of the IP address is true, 
			 * so we need to find out how far to jump.
			 */
			if (multiByte) {
			    jump = jump( me.ip_db[position],
					 me.ip_db[position + 1],
					 me.ip_db[position + 2] );
			} else {
			    jump = jump( me.ip_db[position] );
			}
		    }
		    
		    // ** here's where the jump occurs **
		    position = position + recordLength + jump;
		    
		} else {
		    // this record is not a jump distance
		    // (we should never reach here)
		    throw new InetAddressLocatorException("IP database is corrupt - please re-install");
		}
		
		/*
		 * Check whether the next record in the IP database
		 * contains a terminal node (country code), or a binary
		 * branch (jump). A country code contains information
		 * on which Locale object to return for a particular
		 * range of IP addresses.
		 */
		if (isCountryCode(me.ip_db[position])) {
		    int cc;
		    
		    /*
		     * Country codes are stored in either one or two 
		     * bytes, depending on whether bit1 of the current
		     * database record is set. During database 
		     * construction (not documented here), we ensure
		     * that poupular country codes are held in a single
		     * byte.
		     */
		    if (isMultiByteCountry(me.ip_db[position])) {
			cc = country(me.ip_db[position], me.ip_db[position + 1]);
		    } else {
			cc = country(me.ip_db[position]);
		    }
		    return me.cc_db[cc];
		}
	    }
	}
	// should never reach here, as every IP address is covered by DB
	throw new InetAddressLocatorException("IP database is corrupt - please re-install");
    }
    
    // country codes have bit0 set
    private static boolean isCountryCode(byte b)
	throws InetAddressLocatorException {
	return bitIsSet(b, 0);
    }
    
    // multibyte country code records have bit1 set
    private static boolean isMultiByteCountry(byte b)
	throws InetAddressLocatorException {
	return bitIsSet(b, 1);
    }
    
    // jump records have bit0 unset
    private static boolean isJump(byte b) throws InetAddressLocatorException {
	return !bitIsSet(b, 0);
    }
    
    // mutibyte jump records have bit1 unset
    private static boolean isMultiByteJump(byte b)
	throws InetAddressLocatorException {
	return !bitIsSet(b, 1);
    }
    
    /**
     * single-byte jump records have bit0 unset
     * and bit 1 set. Subtracting 64 converts to
     * a number between 0 and 63.
     */
    private static int jump(byte b) {
	return b - 64;
    }
    
    /**
     * three-byte jump records have neither bit0 or bit1
     * set. Thus, the jump distance is simply:
     *   (byte1 * 2**16) + (byte2 * 2**8) + byte3
     * after each byte has been converted to the range (0 to 255).
     */
    private static int jump(byte one, byte two, byte three)
	throws InetAddressLocatorException {
	return 
	    complementToUnsigned(one) * 256 * 256 + 
	    complementToUnsigned(two) * 256 +
	    complementToUnsigned(three);
    }
    
    /**
     * single-byte country codes have bit0 and bit1 set.
     * adding 128 converts to a number between 0 and 63
     */
    private static int country(byte b) {
	return b + 128;
    }
    
    /**
     * two-byte country codes store the country wholly within
     * the second byte
     */
    private static int country(byte one, byte two)
	throws InetAddressLocatorException {
	return complementToUnsigned(two);
    }
    
    /**
     * helper method to check whether a particular bit of
     * a byte is set. Bit position is numbered from 0 to 7
     * (left to right). Thus,
     *
     *   01001000 binary is 72 decimal
     *   bitIsSet(64,0) returns false
     *   bitIsSet(64,1) returns true
     *
     *   10000010 binary is -126 decimal
     *   bitIsSet(-126,6) returns true
     *   bitIsSet(-126,7) returns false
     *
     * all negative bytes (-128 to -1) return true for bit 0.
     * zero and all positive bytes return false for bit 0.
     */
    private static boolean bitIsSet(byte b, int position)
	throws InetAddressLocatorException {
	
	byte bitPattern;
	
	switch (position) {
	case 0 :
	    bitPattern = -128;
	    break;
	case 1 :
	    bitPattern = 64;
	    break;
	case 2 :
	    bitPattern = 32;
	    break;
	case 3 :
	    bitPattern = 16;
	    break;
	case 4 :
	    bitPattern = 8;
	    break;
	case 5 :
	    bitPattern = 4;
	    break;
	case 6 :
	    bitPattern = 2;
	    break;
	case 7 :
	    bitPattern = 1;
	    break;
	default :
	    throw new InetAddressLocatorException("attempt to check bad bit position");
	}
	
	return ((b & bitPattern) == bitPattern);
    }
    
    /**
     * Java primitive integral types are stored in two's complement
     * format. This private helper method converts between this format
     * and the unsigned equivalent [i.e. changes the range for a byte
     * from (-128 to 127) to (0 to 255)].
     * 
     *   INPUT  OUTPUT
     *      0       0
     *      1       1
     *      2       2
     *      3       3
     *    ...     ...
     *    125     125
     *    126     126
     *    127     127
     *
     *   -128     128
     *   -127     129
     *   -126     130
     *    ...     ...
     *     -3     253
     *     -2     254
     *     -1     255
     * 
     * This method is needed because the database is created in Perl,
     * which uses unsigned (0 to 255) values for bytes. Unsigned bytes 
     * are easily constructed in other common languages, but Java needs
     * a little help.] Note the wider return type to hold the larger-
     * than-byte numbers that occur when converting negative bytes.
     */

    private static int complementToUnsigned(byte b)
	throws InetAddressLocatorException {
	if (bitIsSet(b, 0)) {
	    return b + 256;
	} else {
	    return b;
	}
    }
    
    /**
     * does the opposite of complementToUnsigned().
     * useful method when reading an InputStream using the read()
     * method, which returns an int (0 to 255) instead of returning a 
     * byte (-128 to 127).
     */
    private static byte unsignedToComplement(int i) {
	if ((i & 128) == 128) {
	    return (byte) (i - 256);
	} else {
	    return (byte) i;
	}
    }

    /*
     * Converts IPv4 address in its textual presentation form 
     * into a byte array, which is easier to process. Returns
     * null if parameter is not an IP address.
     *
     * We need this here rather than calling it 
     * from Inet4Address.textToNumericFormat(String)
     * because it's only existed in the JDK since 1.4. Delete from 
     * this class once 1.4 has become the norm.
     * 
     * @param src a String representing an IPv4 address in dotted-quad
     * format
     * @return a byte array representing the IPv4 numeric address
     */
    private static byte[] addressToByteArray(String src)
    {
        if (src.length() == 0) {
            return null;
        }

        int octets;
        char ch;
        byte[] dst = new byte[4];
        char[] srcb = src.toCharArray();
        boolean saw_digit = false;
	
        octets = 0;
        int i = 0;
        int cur = 0;

        while (i < srcb.length) {
            ch = srcb[i++];
            if (Character.isDigit(ch)) {
                int sum =  dst[cur]*10 + (Character.digit(ch, 10) & 0xff);
                if (sum > 255){
                    return null;
		}
                dst[cur] = (byte)(sum & 0xff);
                if (! saw_digit) {
                    if (++octets > 4){
                        return null;
		    }
                    saw_digit = true;
                }
            } else if (ch == '.' && saw_digit) {
                if (octets == 4) {
                    return null;
		}
                cur++;
                dst[cur] = 0;
                saw_digit = false;
            } else {
                return null;
	    }
        }
	
        if (octets != 4) {
            return null;
	}
        return dst;
    }

    /**
     * Runs testsuite, benchmarks, and then enters interactive mode,
     * where the user can query the country of individual IP addresses
     * or hostnames from the console.
     * 
     * @param args command-line arguments currently have no effect
     * @throws InetAddressLocatorException errors occuring during tests and benchmarks
     **/
    public static void main(String[] args) throws InetAddressLocatorException {
	System.out.println();
	System.out.println("  InetAddressLocator - Java country lookup.");
	System.out.println("  -----------------------------------------");
	System.out.println("  Copyright (C) 2003-05 Nigel Wetters Gourlay and Paul Hastings");
	System.out.println("  InetAddressLocator comes with ABSOLUTELY NO WARRANTY.");
	System.out.println("  This is free software, and you are welcome to redistribute");
	System.out.println("  it under the GNU General Public License.");


	System.out.println();
	bench();
	console();
    }
    
    private static void console() {
	String hostname = null;
	System.out.println("  Locating localhost");
	try {
	    hostname = InetAddress.getLocalHost().getHostAddress();
	} catch (java.net.UnknownHostException e) {
	    // warn, but continue
	    System.out.println("  ** resolver error: " + e.getMessage());
	    System.out.println("  continuing...");
	    hostname = "127.0.0.1";
	}
	
	BufferedReader is;
	is = new BufferedReader(new InputStreamReader(System.in));
	
	/*
	 * Located the local machine locale, then prompts the user
	 * for hostnames to locate until s/he enters a 'q' or EOF 
	 * (ctrl-D on Unix) is encountered.
	 */
	while ((hostname != null) && (!hostname.equals("q"))) {
	    InetAddress ip = null;
	    try {
		ip = java.net.InetAddress.getByName(hostname);
		System.out.println("    Host: " + ip.toString());
		try {
		    Locale loc = getLocale(hostname);
		    if (loc.getCountry().equals("")) {
			System.out.println("    Country: Unknown");
		    } else {
			if (loc.getCountry().equals("**")) {
			    System.out.println("    Country: Unknown (private network)");
			} else {
			    System.out.println("    Country:  " + loc.getDisplayCountry());
			    System.out.println("    Language: " + loc.getDisplayLanguage());
			}
		    }
		} catch (InetAddressLocatorException e) {
		    // warn, but continue
		    System.out.println("    ** " + e.getMessage());
		}
		
	    } catch (java.net.UnknownHostException e) {
		// warn, but continue
		System.out.println("    ** could not find hostname **");
	    }
	    
	    System.out.println();
	    System.out.print("  enter host ('q' to quit) > ");
	    
	    try {
		while (((hostname = is.readLine()) != null)
		       && (hostname.equals(""))) {
		    /*
		     * prompt until non-empty, non-null line encountered.
		     * this will loop if user types a single return character,
		     * but will break out of loop if user types EOF character (ctrl-D)
		     */
		    System.out.print("  enter host ('q' to quit) > ");
		}
	    } catch (IOException e) {
		// warn, but continue
		System.out.println("error reading standard input: " + e.getMessage());
	    }
	}
	
	try {
	    is.close();
	} catch (IOException e) {
	    // warn, but continue
	    System.out.println("error closing standard input: " + e.getMessage());
	}
	
	System.out.println();
	System.out.println("  Bye Bye.");
	System.out.println();
	
    }
    
    private static void bench() throws InetAddressLocatorException {
	System.out.println("  Running benchmark. Please be patient...");
	InetAddress[] test_IPs = new java.net.InetAddress[65536];
	java.util.Random r = new java.util.Random();
	
	/*
	 * build the array of test addresses first, so we're sure that we're
	 * timing the address lookup rather than array construction time.
	 */
	try {
	    for (int i = 0; i < test_IPs.length; i++) {
		// random IPv4 address between 0.0.0.0 and 255.255.255.255
		test_IPs[i] =
		    java.net.InetAddress.getByName(r.nextInt(256) + "." +
						   r.nextInt(256) + "." +
						   r.nextInt(256) + "." +
						   r.nextInt(256));
	    }
	} catch (java.net.UnknownHostException e) {
	    /*
	     * should never happen, because hostname DNS lookup should not
	     * occur with dotted-quad hostnames.
	     */
	    throw new InetAddressLocatorException("name resolver error in benchmark",e);
	}
	
	/*
	 * benchmarking occurs here
	 */
	long start = System.currentTimeMillis();
	int found = 0;
	Locale result;
	for (int i = 0; i < test_IPs.length; i++) {
	    result = getLocale(test_IPs[i]);
	    if (result.getCountry().equals("")) {
		// undefined country
	    } else {
		found++;
	    }
	}
	long end = System.currentTimeMillis();
	
	/*
	 * Calculate location rate
	 */
	long delta = 1;
	if (end > start) { // avoids zero division
	    delta = end - start;
	}
	int rate = (int) (test_IPs.length * 1000 / delta);
	int coverage = (found * 100) / test_IPs.length;
	
	/*
	 * Output the results
	 */
	System.out.println("    Speed: " + rate + " ops/sec");
	System.out.println("           (" + coverage + "% of address space is allocated)");
	System.out.println();
    }
    
    /*
     * END OF CLASS
     */
}
