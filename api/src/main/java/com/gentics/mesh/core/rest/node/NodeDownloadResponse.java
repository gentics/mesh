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

	public void setBuffer(Buffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * Return the buffer that contains the binary data.
	 * 
	 * @return
	 */
	public Buffer getBuffer() {
		return buffer;
	}

	/**
	 * Return the content type of the downloaded file.
	 * 
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set the contenttype.
	 * 
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Set the binary filename.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

}
