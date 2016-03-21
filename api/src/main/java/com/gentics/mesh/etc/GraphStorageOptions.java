package com.gentics.mesh.etc;

import java.io.File;

import com.google.gson.JsonObject;

/**
 * Underlying graph database storage configuration.
 */
public class GraphStorageOptions {

	public static final String DEFAULT_DIRECTORY = "data" + File.separator + "graphdb";
	public static final String DEFAULT_BACKUP_DIRECTORY = "data" + File.separator + "backup";
	public static final String DEFAULT_EXPORT_DIRECTORY = "data" + File.separator + "export";

	private String directory = DEFAULT_DIRECTORY;
	private String backupDirectory = DEFAULT_BACKUP_DIRECTORY;
	private String exportDirectory = DEFAULT_EXPORT_DIRECTORY;

	private Boolean startServer = false;

	private JsonObject parameters;

	/**
	 * Return the graph storage directory.
	 * 
	 * @return Graph storage filesystem directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Set the graph storage directory.
	 * 
	 * @param directory
	 *            Graph storage filesystem directory
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * Return custom JSON parameters which can be used to add individual settings for the specific graph provider.
	 * 
	 * @return Additional JSON parameters
	 */
	public JsonObject getParameters() {
		return parameters;
	}

	/**
	 * Set the custom JSON parameters for the selected graph provider.
	 * 
	 * @param parameters
	 *            Additional JSON parameters
	 */
	public void setParameters(JsonObject parameters) {
		this.parameters = parameters;
	}

	/**
	 * Return the backup directory location.
	 * 
	 * @return Backup directory
	 */
	public String getBackupDirectory() {
		return backupDirectory;
	}

	/**
	 * Set the backup directory location.
	 * 
	 * @param backupDirectory
	 *            Backup directory
	 */
	public void setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	/**
	 * Return the graph export directory.
	 * 
	 * @return Export directory
	 */
	public String getExportDirectory() {
		return exportDirectory;
	}

	/**
	 * Set the export directory.
	 * 
	 * @param exportDirectory
	 *            Export directory
	 */
	public void setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}

	/**
	 * Return the start server flag.
	 * 
	 * @return
	 */
	public Boolean getStartServer() {
		return startServer;
	}

	/**
	 * Set the start server flag.
	 * 
	 * @param startServer
	 */
	public void setStartServer(Boolean startServer) {
		this.startServer = startServer;
	}
}
