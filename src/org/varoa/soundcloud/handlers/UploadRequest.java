package org.varoa.soundcloud.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.fileupload.MultipartStream;
import org.varoa.soundcloud.SuperUploaderException;

/**
 * Models an upload request that encapsulates all server side
 * decisions about where and how to store the file.
 */
public class UploadRequest {

	private MultipartStream multipartStream = null;
	private File docRoot = null;
	private String sessionId = null;
	private String fileName = null;
	private CountingFileOutputStream os = null;
	private long size = 0;

	/**
	 * Construct pointing at the root folder for storage.
	 * @param docRoot
	 */
	UploadRequest (File docRoot) {
		this.docRoot = docRoot;
	}
	
	/**
	 * Set the session id associated to this upload.
	 * @param sessionId
	 */
	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	/**
	 * The session id.
	 * @return
	 */
	String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Set the file name.
	 * @param file
	 */
	void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Approx. expected size of file.
	 * @param size
	 */
	void setApproxSize(int size) {
		this.size = size;
	}
	
	/**
	 * Confirm that we have all data needed.
	 * @return
	 */
	boolean isComplete() {
		return (this.sessionId != null) && (this.fileName != null);
	}
	

	/**
	 * Construct an output stream where data should be posted to.
	 * @return
	 */
	void setMultipartStream(MultipartStream multipartStream) {
		this.multipartStream = multipartStream;
	}
	
	/**
	 * Get approx. % of upload completed
	 * @return
	 */
	float getCompletedPercent() {
		long current = this.os.getCount();		
		if (this.size == 0) {
			return 0;
		} else {
			return (float)(100 * current) / this.size;
		}
	}
	
	/**
	 * Do the job.
	 */
	void process() throws IOException, SuperUploaderException {		
		if (!this.isComplete()) {
			throw new SuperUploaderException("Upload request not complete - session: " + this.sessionId + " filename: " + this.fileName + " multipart stream " + this.multipartStream);
		}
		File sessionFolder = new File(this.docRoot, this.sessionId);
		if (!sessionFolder.exists() && !sessionFolder.mkdir()) {
			throw new SuperUploaderException("Can't create dest. folder: " + sessionFolder);
		}
		final File file = new File(sessionFolder, this.fileName);
		if (!file.exists() && !file.createNewFile()) {
			throw new SuperUploaderException("Destination file cannot be created: " + file);
		}
		this.os = new CountingFileOutputStream(file);
		this.multipartStream.readBodyData(os);
		this.os.close();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("UploadRequest {");
		sb.append(this.sessionId).append(", ");
		sb.append(this.fileName).append("}");
		return sb.toString();
	}

	/**
	 * TODO: investigate why extending {@link FileOutputStream} doesn't work
	 * with the same override.
	 */
	private class CountingFileOutputStream extends OutputStream {
		
		private long count = 0;
		private FileOutputStream fos = null;

		/**
		 * Construct forwarding all input to the given file.
		 */
		public CountingFileOutputStream(File file) throws FileNotFoundException {
			this.fos = new FileOutputStream(file);
		}

		/**
		 * Return the count of bytes written to this OutputStream up to now.
		 * @return
		 */
		public long getCount() {
			return count;
		}

		/**
		 * Proxy to an internal {@link FileOutputStream} but counting the byte.
		 */
		@Override 
		public void write(int b) throws IOException {
			this.fos.write(b);
			this.count++;
		}
	}

}
