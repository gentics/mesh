package com.gentics.mesh.etc;

import com.google.gson.JsonObject;

/**
 * Underlying graph database storage configuration
 */
public class StorageOptions {

	public static final String DEFAULT_DIRECTORY = "/tmp/graphdb";

	private String directory = DEFAULT_DIRECTORY;

	private JsonObject parameters;

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public JsonObject getParameters() {
		return parameters;
	}

	public void setParameters(JsonObject parameters) {
		this.parameters = parameters;
	}

}
