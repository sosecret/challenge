package org.varoa.soundcloud.handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.varoa.soundcloud.SessionExtractor;
import org.varoa.soundcloud.SuperUploaderException;

/**
 * Attends upload requests for the current session, store the file and
 * ensure that progress is tracked in the {@link UploadRequestTracker} singleton.
 */
public class UploadHandler implements HttpAsyncRequestHandler<HttpRequest> {

	private static Logger log = Logger.getLogger(UploadHandler.class);
	private File docRoot = null;

	public UploadHandler(final File docRoot) {
		this.docRoot = docRoot;
	}

	@Override 
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context) {
		// Buffer request content in memory for simplicity
		return new BasicAsyncRequestConsumer();
	}

	@Override 
	final public void handle(final HttpRequest request, final HttpAsyncExchange httpexchange, final HttpContext context) throws HttpException, IOException {
		
		HttpResponse response = httpexchange.getResponse();
		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
		if (!"POST".equals(method)) {
			throw new MethodNotSupportedException(method + " method not supported");
		}
		
		HttpEntity entity = null;
		if (request instanceof HttpEntityEnclosingRequest) {
			// awful documentation from Apache, found on: 
			// http://www.oreillynet.com/onjava/blog/2006/06/parsing_formdata_multiparts.html
			entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String contentType = entity.getContentType().toString();
			int boundaryIndex = contentType.indexOf("boundary=");
			byte[] boundary = (contentType.substring(boundaryIndex + 9)).getBytes();
			// prepare multipart stream
			ByteArrayInputStream input = new ByteArrayInputStream(EntityUtils.toByteArray(entity));
			// had no time to investigate this one, the documentation for
			// HttpCore and FileUpload is dismal.
			@SuppressWarnings("deprecation")
			MultipartStream multipartStream = new MultipartStream(input, boundary);
			multipartStream.skipPreamble();
			// extract data
			String headers = multipartStream.readHeaders();
			String fileName = headers.substring(headers.indexOf("filename=") + 10, headers.lastIndexOf("\""));
			String sessionId = SessionExtractor.extractSession(context);
			// create upload request
			UploadRequest uploadReq = new UploadRequest(this.docRoot);
			uploadReq.setApproxSize(input.available());
			uploadReq.setSessionId(sessionId);
			uploadReq.setFileName(fileName);
			uploadReq.setMultipartStream(multipartStream);
			// process it
			try {
				UploadRequestTracker.getInstance().addUploadRequest(uploadReq);
				uploadReq.process();
				log.info("Completed " + uploadReq);
				log.info(uploadReq);
				NStringEntity stringEntity = new NStringEntity("OK");
				response.setEntity(stringEntity);
				httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
				UploadRequestTracker.getInstance().removeUploadRequest(uploadReq);
			} catch (SuperUploaderException e) {
				log.error("Error processing upload request", e);
				this.handleBadRequest(response);
			}
			
		} else {
			log.error("Non HttpEntityEnclosingRequest (!)");
		}

	}
	
	/**
	 * Sends a 500 error.
	 * 
	 * @param file
	 * @param response
	 * @param context
	 */
	private void handleBadRequest(final HttpResponse response) {
	    response.addHeader("Expires", "0");
	    response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
	    response.addHeader("Pragma", "no-cache");
		response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		NStringEntity entity = new NStringEntity("<html><body><h1>Internal Server Error</h1></body></html>", ContentType.create("text/html", "UTF-8"));
		response.setEntity(entity);
	}
	
}
