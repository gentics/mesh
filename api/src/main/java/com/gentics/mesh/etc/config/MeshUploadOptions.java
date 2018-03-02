package com.gentics.mesh.etc.config;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

@GenerateDocumentation
public class MeshUploadOptions implements Option {

	public static final long DEFAULT_FILEUPLOAD_MB_LIMIT = 250; // 250 MiB
	public static final long DEFAULT_FILEUPLOAD_BYTE_LIMIT = 1024 * 1024 * DEFAULT_FILEUPLOAD_MB_LIMIT;
	public static final String DEFAULT_BINARY_DIRECTORY = "data" + File.separator + "binaryFiles";
	public static final String DEFAULT_TEMP_DIR = "data" + File.separator + "tmp" + File.separator + "file-uploads";;

	public static final String MESH_BINARY_DIR_ENV = "MESH_BINARY_DIR";
	public static final String MESH_BINARY_UPLOAD_TEMP_DIR_ENV = "MESH_BINARY_UPLOAD_TEMP_DIR";

	@JsonProperty(required = false)
	@JsonPropertyDescription("The upload size limit in bytes. Default: " + DEFAULT_FILEUPLOAD_MB_LIMIT)
	@EnvironmentVariable(name = MESH_BINARY_DIR_ENV, description = "Override the configured binary byte upload limit.")
	private long byteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path binary data storage directory. Fileuploads will be placed here.")
	@EnvironmentVariable(name = MESH_BINARY_DIR_ENV, description = "Override the configured binary data directory.")
	private String directory = DEFAULT_BINARY_DIRECTORY;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the file upload temporary directory. Inbound file uploads will be placed here before they are processed.")
	@EnvironmentVariable(name = MESH_BINARY_UPLOAD_TEMP_DIR_ENV, description = "Override the configured upload temporary directory.")
	private String tempDirectory = DEFAULT_TEMP_DIR;

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
