package com.gentics.mesh.core.endpoint.node;

import io.vertx.ext.web.FileUpload;

public class UploadContext {

	private String binaryUuid;

	private FileUpload ul;

	private String hash;

	public UploadContext() {
	}

	public void setBinaryUuid(String binaryUuid) {
		this.binaryUuid = binaryUuid;
	}

	public String getBinaryUuid() {
		return binaryUuid;
	}

	public void setUpload(FileUpload ul) {
		this.ul = ul;
	}

	public FileUpload getUpload() {
		return ul;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getHash() {
		return hash;
	}

}
