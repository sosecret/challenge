package org.varoa.soundcloud.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.varoa.soundcloud.SessionExtractor;

/**
 * Deals with requests that want to store comments in the current session.
 */
public class CommentHandler implements HttpAsyncRequestHandler<HttpRequest> {

	private static Logger log = Logger.getLogger(CommentHandler.class);
	private File docRoot = null;

	public CommentHandler(final File docRoot) {
		this.docRoot = docRoot;
	}

	public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context) {
		// Buffer request content in memory for simplicity
		return new BasicAsyncRequestConsumer();
	}

	/**
	 * Implements contract from {@link HttpAsyncRequestHandler}.
	 */
	final public void handle(final HttpRequest request, final HttpAsyncExchange httpExchange, final HttpContext context) throws HttpException, IOException {
        String sessionId = SessionExtractor.extractSession(context);
        HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
        byte[] data = (entity == null) ? new byte [0] : EntityUtils.toByteArray(entity);
        String sData = new String(data);
        sData = sData.substring(sData.indexOf("=") + 1);
        log.info("Storing comment for session " + sessionId + ": " + sData);
        FileOutputStream fos = new FileOutputStream(new File(this.docRoot, sessionId + "/comment.txt"));
        fos.write(sData.getBytes());
        fos.close();
        HttpResponse response = httpExchange.getResponse();
        response.setEntity(new StringEntity("OK"));
		httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
	}

}
