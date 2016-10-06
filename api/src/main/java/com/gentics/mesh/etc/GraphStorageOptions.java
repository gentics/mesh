package com.gentics.mesh.etc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

	private Map<String, String> parameters = new HashMap<>();

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
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Set the an additional custom parameters for the selected graph provider.
	 * 
	 * @param key
	 * @param value
	 */
	public void setParameter(String key, String value) {
		this.parameters.put(key, value);
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
