/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.varoa.soundcloud.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

/**
 * Serves a file, modified from the example so it can be used for any file and 
 * used both in the {@link WelcomeHandler} and {@link DownloadHandler}.
 * 
 * Based on the example provided with HTTPCore at
 * http://hc.apache.org/httpcomponents-core-ga/httpcore-nio/index.html
 */
abstract class HttpFileHandler implements HttpAsyncRequestHandler<HttpRequest> {

	private static Logger log = Logger.getLogger(HttpFileHandler.class);

	public HttpFileHandler() {
		super();
	}

	@Override 
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context) {
		// Buffer request content in memory for simplicity
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(final HttpRequest request, final HttpAsyncExchange httpexchange, final HttpContext context) throws HttpException, IOException {
		HttpResponse response = httpexchange.getResponse();
		handleInternal(request, response, context);
		httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
	}

	/**
	 * Coordinates the various actions involved in serving the request. 
	 * 
	 * Spreads concrete actions in methods to allow overriding.
	 * 
	 * @param request
	 * @param response
	 * @param context
	 * @throws HttpException
	 * @throws IOException
	 */
	private void handleInternal(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {

		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
		if (!"GET".equals(method) && !"HEAD".equals(method) && !"POST".equals(method)) {
			throw new MethodNotSupportedException(method + " method not supported");
		}
	    response.addHeader("Expires", "0");
	    response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
	    response.addHeader("Pragma", "no-cache");
		final File file = this.getRequestedFile(request);
		if (file == null || !file.canRead() || file.isDirectory()) {
			this.handleAccessDenied(file, response, context);
		} else if (!file.exists()) {
			this.handleNotExists(file, response, context);
		} else {
			this.handleServeFile(file, request, response, context);
		}
	}

	/**
	 * Serves the file.
	 * 
	 * @param file
	 * @param response
	 * @param context
	 */
	protected void handleServeFile(final File file, final HttpRequest request, final HttpResponse response, final HttpContext context) {
		NHttpConnection conn = (NHttpConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
		response.setStatusCode(HttpStatus.SC_OK);
		response.setEntity(this.getNFileEntity(file));
		log.info(conn + ": serving file " + file.getPath());
	}

	/**
	 * Sends a 403.
	 * 
	 * @param file
	 * @param response
	 * @param context
	 */
	final protected void handleAccessDenied(final File file, final HttpResponse response, final HttpContext context) {
		response.setStatusCode(HttpStatus.SC_FORBIDDEN);
		NStringEntity entity = new NStringEntity("<html><body><h1>Access denied</h1></body></html>", ContentType.create("text/html", "UTF-8"));
		response.setEntity(entity);
		log.info("Cannot read file " + file.getName());
	}

	/**
	 * Sends a 404.
	 * 
	 * @param file
	 * @param response
	 * @param context
	 */
	final protected void handleNotExists(final File file, final HttpResponse response, final HttpContext context) {
		response.setStatusCode(HttpStatus.SC_NOT_FOUND);
		NStringEntity entity = new NStringEntity("<html><body><h1>File" + file.getPath() + " not found</h1></body></html>", ContentType.create("text/html",
				"UTF-8"));
		response.setEntity(entity);
		log.info("File " + file.getName() + " not found");
	}

	/**
	 * Tell the file that we want to serve, it's assumed that the subclass will
	 * take care to ensure that we do have rights to access etc.
	 * 
	 * @param request
	 * @return
	 */
	abstract protected File getRequestedFile(final HttpRequest request);

	/**
	 * Set the file and the type if known in the implementing handler.
	 * 
	 * @param f
	 * @return
	 */
	abstract protected NFileEntity getNFileEntity(final File f);

}
