package com.gentics.mesh.core.rest.node;

import io.vertx.core.buffer.Buffer;

/**
 * POJO for a node binary download response model.
 */
public class NodeDownloadResponse {

	private Buffer buffer;
	private String contentType;
	private String filename;

	public NodeDownloadResponse() {
	}

	/**
	 * Return the buffer that contains the binary data.
	 * 
	 * @return Buffer that contains the binary data
	 */
	public Buffer getBuffer() {
		return buffer;
	}

	/**
	 * Set the buffer that contains the binary data.
	 * 
	 * @param buffer
	 *            Buffer that contains the binary data
	 */
	public void setBuffer(Buffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * Return the content type of the downloaded file.
	 * 
	 * @return Contenttype of the download
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set the contenttype.
	 * 
	 * @param contentType
	 *            Contenttype of the download
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the binary filename.
	 * 
	 * @return Binary filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Set the binary filename.
	 * 
	 * @param filename
	 *            Binary filename
	 * 
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

}
