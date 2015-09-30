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

	/**
	 * Set the upload limit in bytes.
	 * 
	 * @param byteLimit
	 */
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

	/**
	 * Set the binary storage directory.
	 * 
	 * @param directory
	 */
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

	/**
	 * Set the temporary upload directory. New uploads will be placed within this location before processing.
	 * 
	 * @param tempDirectory
	 */
	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

}
