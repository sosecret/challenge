package org.varoa.soundcloud;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

/**
 * The challenge doc didn't specify much how sessions should work,
 * since it's supposed to be for each user I assumed IP-per-user
 * for simplicity.
 */
public final class SessionExtractor {

	public static final String extractSession(HttpContext ctx) {
        String remoteAddr = ctx.getAttribute(ExecutionContext.HTTP_CONNECTION).toString();
        try {
        	remoteAddr = remoteAddr.substring(0, remoteAddr.indexOf(":"));
        	byte[] msgDigest = MessageDigest.getInstance("MD5").digest();
        	StringBuilder hexString = new StringBuilder();
        	for (int i=0 ; i<msgDigest.length ; i++) {
        		hexString.append(Integer.toHexString(0xFF & msgDigest[i]));
        	}
        	return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Logger.getLogger(SessionExtractor.class).error("Cannot instantiate MD5 MessageDigest!");
			return null;
		}
	}
}
