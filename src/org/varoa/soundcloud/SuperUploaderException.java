package org.varoa.soundcloud;

/**
 * Generic exception for any error inside the server, obviously
 * should be specialized depending on needs.
 */
public class SuperUploaderException extends Exception {
	
	private static final long serialVersionUID = 3369024486342854984L;

	public SuperUploaderException (String message) {
		super(message);
	}
	
	public SuperUploaderException (String message, Throwable cause) {
		super(message, cause);
	}

}
