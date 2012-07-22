package org.varoa.soundcloud.handlers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.varoa.soundcloud.SessionExtractor;

/**
 * Serves files uploaded. Will use the current session id and expect that the
 * file exists in the same session id. Code should be self explanatory.
 * 
 * Obviously if we had some session management for the user we'd use that
 * instead of the session id but adding more user session logic was out of
 * scope.
 */
public class DownloadHandler extends HttpFileHandler {

	private static Logger log = Logger.getLogger(DownloadHandler.class);
	private File docRoot = null;
	private String sessionId = null;

	public DownloadHandler(final File docRoot) {
		this.docRoot = docRoot;
	}
	
	@Override
	protected void handleServeFile(final File file, final HttpRequest request, final HttpResponse response, final HttpContext context) {
		super.handleServeFile(file, request, response, context);
		this.sessionId = SessionExtractor.extractSession(context);
	}

	@Override
	protected NFileEntity getNFileEntity(final File f) {
		String mimeType = URLConnection.guessContentTypeFromName(f.getName());
		return new NFileEntity(f, ContentType.create(mimeType));
	}

	@Override
	protected File getRequestedFile(final HttpRequest request) {
		String target = request.getRequestLine().getUri().replaceAll("/download/", "");
		try {
			File sessionStorage = new File(this.docRoot, sessionId);
			final File file = new File(sessionStorage, URLDecoder.decode(target, "UTF-8"));
			return file;
		} catch (UnsupportedEncodingException e) {
			log.error("Could not decode URL: ", e);
			return null;
		}
	}

}
