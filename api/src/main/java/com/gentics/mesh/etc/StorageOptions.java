package com.gentics.mesh.etc;

import com.google.gson.JsonObject;

/**
 * Underlying graph database storage configuration
 */
public class StorageOptions {

	public static final String DEFAULT_DIRECTORY = "mesh-graphdb";
	public static final String DEFAULT_BACKUP_DIRECTORY = "mesh-backup";
	public static final String DEFAULT_EXPORT_DIRECTORY = "mesh-export";

	private String directory = DEFAULT_DIRECTORY;
	private String backupDirectory = DEFAULT_BACKUP_DIRECTORY;
	private String exportDirectory = DEFAULT_EXPORT_DIRECTORY;

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

	public String getBackupDirectory() {
		return backupDirectory;
	}

	public void setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	public String getExportDirectory() {
		return exportDirectory;
	}

	public void setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}
}
