package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.FileUpload;

public class UploadContext {

	private String binaryUuid;

	private FileUpload ul;

	private String hash;

	private String temporaryId;

	private boolean invokeStore = false;

	public UploadContext() {
		this.temporaryId = UUIDUtil.randomUUID();
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

	public String getTemporaryId() {
		return temporaryId;
	}

	public void setInvokeStore() {
		this.invokeStore = true;
	}

	public boolean isInvokeStore() {
		return this.invokeStore;
	}

}
