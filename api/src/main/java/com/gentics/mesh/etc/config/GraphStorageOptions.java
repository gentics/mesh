package com.gentics.mesh.etc.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Underlying graph database storage configuration.
 */
@GenerateDocumentation
public class GraphStorageOptions implements Option {

	public static final String DEFAULT_DIRECTORY = "data" + File.separator + "graphdb";
	public static final String DEFAULT_BACKUP_DIRECTORY = "data" + File.separator + "backup";
	public static final String DEFAULT_EXPORT_DIRECTORY = "data" + File.separator + "export";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database data directory.")
	@EnvironmentVariable(name = "GRAPH_DB_DIRECTORY", description = "Override the graph database storage directory.")
	private String directory = DEFAULT_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database backup directory.")
	@EnvironmentVariable(name = "GRAPH_BACKUP_DIRECTORY", description = "Override the graph database backup directory.")
	private String backupDirectory = DEFAULT_BACKUP_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database export directory.")
	private String exportDirectory = DEFAULT_EXPORT_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the graph database admin web server should be started.")
	@EnvironmentVariable(name = "GRAPH_STARTSERVER", description = "Override the graph database server flag.")
	private Boolean startServer = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional set of graph database parameters.")
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
	 * @return Fluent API
	 */
	public GraphStorageOptions setDirectory(String directory) {
		this.directory = directory;
		return this;
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
	 * @return Fluent API
	 */
	public GraphStorageOptions setParameter(String key, String value) {
		this.parameters.put(key, value);
		return this;
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
	 * @return Fluent API
	 */
	public GraphStorageOptions setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
		return this;
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
	 * @return Fluent API
	 */
	public GraphStorageOptions setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
		return this;
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
	 * @return Fluent API
	 */
	public GraphStorageOptions setStartServer(Boolean startServer) {
		this.startServer = startServer;
		return this;
	}

	public void validate(MeshOptions meshOptions) {
		if (getStartServer() && getDirectory() == null) {
			throw new NullPointerException(
					"You have not specified a data directory and enabled the graph server. It is not possible to run Gentics Mesh in memory mode and start the graph server.");
		}
	}
}
