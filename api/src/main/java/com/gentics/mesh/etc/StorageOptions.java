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

	/**
	 * Return the graph storage directory.
	 * 
	 * @return
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Set the graph storage directory.
	 * 
	 * @param directory
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * Return custom JSON parameters which can be used to add individual settings for the specific graph provider.
	 * 
	 * @return
	 */
	public JsonObject getParameters() {
		return parameters;
	}

	/**
	 * Set the custom JSON parameters for the selected graph provider.
	 * 
	 * @param parameters
	 */
	public void setParameters(JsonObject parameters) {
		this.parameters = parameters;
	}

	/**
	 * Return the backup directory location.
	 * 
	 * @return
	 */
	public String getBackupDirectory() {
		return backupDirectory;
	}

	/**
	 * Set the backup directory location.
	 * 
	 * @param backupDirectory
	 */
	public void setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	/**
	 * Return the graph export directory.
	 * 
	 * @return
	 */
	public String getExportDirectory() {
		return exportDirectory;
	}

	/**
	 * Set the export directory.
	 * 
	 * @param exportDirectory
	 */
	public void setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}
}
