package net.sf.javainetlocator;

public class CFMXwrapper {
    public CFMXwrapper() {
    }
    
    public static java.util.Locale getLocale(String host)
	throws InetAddressLocatorException {
	return InetAddressLocator.getLocale(host);
    }
    
    public static java.util.Locale getLocale(java.net.InetAddress address)
	throws InetAddressLocatorException {
	return InetAddressLocator.getLocale(address);
    }
    
    public static java.util.Locale getLocale(byte[] ip)
	throws InetAddressLocatorException {
	return InetAddressLocator.getLocale(ip);
    }
    
    /*
     * END OF CLASS
     */
}
