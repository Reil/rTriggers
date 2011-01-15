/*
 * @(#)InetAddressLocatorException.java 2.20 02/06/05
 *
 * Copyright 2003 Nigel Wetters Gourlay. All rights reserved.
 */

package net.sf.javainetlocator;

/**
 * Signals that a location exception of some sort has occurred.
 *
 * <p>Copyright 2003 Nigel Wetters Gourlay. All rights reserved.</p>
 * 
 * <p>This program is free software; you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation; either version 2 
 * of the License, or(at your option) any later version.</p>
 * 
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.</p>
 * 
 * <p>You should have received a copy of the GNU General Public License along with this 
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA</p>
 * 
 * @author  Nigel Wetters Gourlay
 * @version 2.20, 02/06/05
 * @see java.lang.Exception
 */
public class InetAddressLocatorException extends Exception {
    // Determines if a de-serialized file is compatible with this class.
    private final static long serialVersionUID = 982369951L;

    /**
     * Constructs an InetAddressLocatorException with null
     * as its error detail message.
     */
    public InetAddressLocatorException() {
	super();
    }

    /**
     * Constructs an InetAddressLocatorException with the specified detail
     * message. The error message string <code>message</code> can later be
     * retrieved by the <code>{@link java.lang.Throwable#getMessage}</code>
     * method of class <code>java.lang.Throwable</code>.
     *
     * @param  message the detail message.
     */
    public InetAddressLocatorException(String message) {
	super(message);
    }

    /**
     * Constructs a new InetAddressLocatorException with the specified detail message and
     * cause. The error message string <code>message</code> can later be
     * retrieved by the <code>{@link java.lang.Throwable#getMessage}</code>
     * method of class <code>java.lang.Throwable</code>.
     *
     * @param  message the detail message.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link java.lang.Throwable#getCause()} method).
     *         (A <tt>null</tt> value is permitted, and indicates that 
     *         the cause is nonexistent or unknown.)
     */
    public InetAddressLocatorException(String message, Throwable cause) {
	super(message, cause);
    }
}
