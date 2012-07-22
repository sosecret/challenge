package org.varoa.soundcloud.handlers;

import java.io.File;

import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NFileEntity;

/**
 * Serves an uploaded file, which is essentially the same as
 * {@link HttpFileHandler} but serving an specific HTML file.
 * 
 * As pointed out in form.html, I just did a quick html file containing
 * all css, js, etc. But this could be easily generalized to read any 
 * requested file inside a public folder, etc. Short on time, so didn't
 * get into it.
 */
public class WelcomeHandler extends HttpFileHandler {

	private File welcomeForm = null;

	public WelcomeHandler(final File welcomeForm) {
		this.welcomeForm = welcomeForm;
	}
		
	@Override
	protected NFileEntity getNFileEntity(final File f) {
		return new NFileEntity(f, ContentType.create("text/html"));
	}

	@Override
	protected File getRequestedFile(final HttpRequest request) {
		return this.welcomeForm;
	}

}
