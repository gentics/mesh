package com.gentics.mesh.core.rest.node;

import io.vertx.core.buffer.Buffer;

public class NodeDownloadResponse {

	private Buffer buffer;
	private String contentType;
	private String filename;

	public NodeDownloadResponse() {
	}

	public void setBuffer(Buffer buffer) {
		this.buffer = buffer;
	}

	public Buffer getBuffer() {
		return buffer;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
