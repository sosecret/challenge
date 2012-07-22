package org.varoa.soundcloud.handlers;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.varoa.soundcloud.SuperUploaderException;

/**
 * Singleton used to track all active upload sessions.
 */
public class UploadRequestTracker {

	private static final Logger log = Logger.getLogger(UploadRequestTracker.class);
	private static UploadRequestTracker instance = null;
	private Map<String, UploadRequest> map = new HashMap<String, UploadRequest>();
	
	public static UploadRequestTracker getInstance() {
		if (UploadRequestTracker.instance == null) {
			UploadRequestTracker.instance = new UploadRequestTracker();
		}
		return UploadRequestTracker.instance;
	}
	
	/**
	 * Adds an upload request for the given session, expects that none exist
	 * already and will throw {@link SuperUploaderException} otherwise.
	 * 
	 * @param sessionId
	 * @param req
	 * @throws SuperUploaderException
	 */
	void addUploadRequest(final UploadRequest req) throws SuperUploaderException { 
		final String sessionId = req.getSessionId();
		log.info("New upload request tracked: " + req);
		if (this.map.containsKey(sessionId)) {
			throw new SuperUploaderException("Upload request already present!");
		}
		this.map.put(sessionId, req);
	}
	
	/**
	 * Removes an upload request for a given session, if exists.
	 * @param uploadReq
	 */
	void removeUploadRequest(final UploadRequest uploadReq) {
		this.map.remove(uploadReq.getSessionId());
	}
	
	/**
	 * Returns the current % stored in the request associated to the given 
	 * session, null if no requests for that session currently active.
	 * 
	 * @param sessionId
	 * @return
	 */
	Float getUploadProgressPercent(final String sessionId) {
		UploadRequest req = this.map.get(sessionId);
		Float res = null;
		if (req != null) {
			res = req.getCompletedPercent();
		}
		return res;
	}
	
}
