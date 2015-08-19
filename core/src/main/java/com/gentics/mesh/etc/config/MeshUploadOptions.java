package com.gentics.mesh.etc.config;

public class MeshUploadOptions {

	public static final long DEFAULT_FILEUPLOAD_BYTE_LIMIT = 1024 * 1024 * 250; // 250 MiB

	private long byteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	private String directory = "binaryFiles";

	public long getByteLimit() {
		return byteLimit;
	}

	public void setByteLimit(long byteLimit) {
		this.byteLimit = byteLimit;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

}
