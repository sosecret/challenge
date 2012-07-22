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
package org.varoa.soundcloud;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.URL;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerRegistry;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.varoa.soundcloud.handlers.CommentHandler;
import org.varoa.soundcloud.handlers.DownloadHandler;
import org.varoa.soundcloud.handlers.ProgressHandler;
import org.varoa.soundcloud.handlers.UploadHandler;
import org.varoa.soundcloud.handlers.WelcomeHandler;

/**
 * Based on the example provided with HTTPCore at 
 * 	http://hc.apache.org/httpcomponents-core-ga/httpcore-nio/index.html
 * 
 * HTTP/1.1 file server based on the non-blocking I/O model and capable of
 * direct channel (zero copy) data transfer.
 * 
 * Could be implemented from scratch without too much effort but 
 * preferred not to reimplement the wheel. Also can benefit from
 * multipart, headers and other parsing.
 */
public class BasicHttpServer {

	private static Logger log = Logger.getLogger(BasicHttpServer.class);

	/**
	 * Main entry point. Requests the route of the folder that contains
	 * the main http file shown to the user.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		PropertyConfigurator.configure("log4j.properties");
		if (args.length < 2) {
			log.error("Usage: and run public_folder storage [port]");
			System.exit(1);
		}		
		
		// Document root directory
		File docRoot = new File(args[0]);
		int port = 80;
		if (args.length >= 3) {
			port = Integer.parseInt(args[2]);
		}
		// HTTP parameters for the server
		HttpParams params = new SyncBasicHttpParams();
		params
			.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
			.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
			.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
			.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "SuperUploader/1.1");
		
		// Create HTTP protocol processing chain
		HttpProcessor httpproc = new ImmutableHttpProcessor(
				new HttpResponseInterceptor[] {
						// Use standard server-side protocol interceptors
						new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl() 
					});
		// Create request handler registry
		HttpAsyncRequestHandlerRegistry reqistry = new HttpAsyncRequestHandlerRegistry();
		
		log.info("Registering handlers..");
		// Register handlers for each action
		File storage = new File(args[1]);
		reqistry.register("/superuploader", new WelcomeHandler(new File(docRoot, "/form.html")));
		reqistry.register("/upload", new UploadHandler(storage));
		reqistry.register("/download/*", new DownloadHandler(storage));
		reqistry.register("/comment", new CommentHandler(storage));
		reqistry.register("/progress", new ProgressHandler());
		
		// Create server-side HTTP protocol handler
		HttpAsyncService protocolHandler = new HttpAsyncService(httpproc, new DefaultConnectionReuseStrategy(), reqistry, params) {

			@Override
			public void connected(final NHttpServerConnection conn) {
				log.info(conn + ": connection open");
				super.connected(conn);
			}

			@Override
			public void closed(final NHttpServerConnection conn) {
				log.info(conn + ": connection closed");
				super.closed(conn);
			}

		};
		
		// Create HTTP connection factory
		NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory;
		connFactory = new DefaultNHttpServerConnectionFactory(params);
		
		// Create server-side I/O event dispatch
		IOEventDispatch ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);
		
		// Create server-side I/O reactor
		log.info("Listening..");
		ListeningIOReactor ioReactor = new DefaultListeningIOReactor();
		try {
			ioReactor.listen(new InetSocketAddress(port));
			ioReactor.execute(ioEventDispatch);
		} catch (InterruptedIOException ex) {
			log.error("Interrupted");
		} catch (IOException e) {
			log.error("I/O error: " + e.getMessage());
		}
		log.info("Shutdown");
		
	}
	
}