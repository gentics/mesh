package com.gentics.mesh.etc.config;

import java.io.File;

public class MeshUploadOptions {

	public static final long DEFAULT_FILEUPLOAD_BYTE_LIMIT = 1024 * 1024 * 250; // 250 MiB

	private long byteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	private String directory = "binaryFiles";

	private String tempDirectory = new File("tmp", "file-uploads").getAbsolutePath();

	/**
	 * Return the upload limit in bytes.
	 * 
	 * @return
	 */
	public long getByteLimit() {
		return byteLimit;
	}

	public void setByteLimit(long byteLimit) {
		this.byteLimit = byteLimit;
	}

	/**
	 * Return the binary storage directory.
	 * 
	 * @return
	 */
	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * Returns the upload temporary directory. New uploads are placed in this directory before those are processed and moved.
	 * 
	 * @return
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

}
