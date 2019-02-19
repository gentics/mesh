package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.core.data.binary.Binary;

import io.vertx.ext.web.FileUpload;

public class UploadContext {

	/**
	 * Flag which indicates whether a store action is needed
	 */
	private boolean store = false;

	/**
	 * Binary vertex which holds the binary reference
	 */
	private Binary binary;

	private String binaryUuid;

	private FileUpload ul;

	public UploadContext() {
	}

	public boolean isStore() {
		return store;
	}

	public void setStore(boolean store) {
		this.store = store;
	}

	public Binary getBinary() {
		return binary;
	}

	public void setBinary(Binary binary) {
		this.binary = binary;
		this.binaryUuid = binary.getUuid();
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

}
