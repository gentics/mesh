package com.gentics.mesh.etc.config;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;

@GenerateDocumentation
public class MeshUploadOptions {

	public static final long DEFAULT_FILEUPLOAD_BYTE_LIMIT = 1024 * 1024 * 250; // 250 MiB

	private long byteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path binary data storage directory. Fileuploads will be placed here.")
	private String directory = "data" + File.separator + "binaryFiles";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the file upload temporary directory. Inbound file uploads will be placed here before they are processed.")
	private String tempDirectory = "data" + File.separator + "tmp" + File.separator + "file-uploads";

	/**
	 * Return the upload limit in bytes.
	 * 
	 * @return Limit in bytes
	 */
	public long getByteLimit() {
		return byteLimit;
	}

	/**
	 * Set the upload limit in bytes.
	 * 
	 * @param byteLimit
	 *            Limit in bytes
	 * @return Fluent API
	 */
	public MeshUploadOptions setByteLimit(long byteLimit) {
		this.byteLimit = byteLimit;
		return this;
	}

	/**
	 * Return the binary storage directory.
	 * 
	 * @return Binary storage filesystem directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Set the binary storage directory.
	 * 
	 * @param directory
	 *            Binary storage filesystem directory
	 * @return Fluent API
	 */
	public MeshUploadOptions setDirectory(String directory) {
		this.directory = directory;
		return this;
	}

	/**
	 * Returns the upload temporary directory. New uploads are placed in this directory before those are processed and moved.
	 * 
	 * @return Temporary filesystem directory
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * Set the temporary upload directory. New uploads will be placed within this location before processing.
	 * 
	 * @param tempDirectory
	 *            Temporary filesystem directory
	 * @return Fluent API
	 */
	public MeshUploadOptions setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
		return this;
	}

}
