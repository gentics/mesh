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
	public static final boolean DEFAULT_START_SERVER = false;
	public static final boolean DEFAULT_SYNC_WRITES = true;
	public static final long DEFAULT_SYNC_WRITES_TIMEOUT = 60_000;
	public static final int DEFAULT_TX_RETRY_DELAY = 10;
	public static final int DEFAULT_TX_RETRY_LIMIT = 10;
	public static final long DEFAULT_TX_COMMIT_TIMEOUT = 0;
	public static final int DEFAULT_CLUSTER_JOIN_TIMEOUT = 500_000;

	public static final String MESH_GRAPH_DB_DIRECTORY_ENV = "MESH_GRAPH_DB_DIRECTORY";
	public static final String MESH_GRAPH_BACKUP_DIRECTORY_ENV = "MESH_GRAPH_BACKUP_DIRECTORY";
	public static final String MESH_GRAPH_EXPORT_DIRECTORY_ENV = "MESH_GRAPH_EXPORT_DIRECTORY";
	public static final String MESH_GRAPH_STARTSERVER_ENV = "MESH_GRAPH_STARTSERVER";
	public static final String MESH_GRAPH_SYNC_WRITES_ENV = "MESH_GRAPH_SYNC_WRITES";
	public static final String MESH_GRAPH_SYNC_WRITES_TIMEOUT_ENV = "MESH_GRAPH_SYNC_WRITES_TIMEOUT";
	public static final String MESH_GRAPH_TX_RETRY_DELAY_ENV = "MESH_GRAPH_TX_RETRY_DELAY";
	public static final String MESH_GRAPH_TX_RETRY_LIMIT_ENV = "MESH_GRAPH_TX_RETRY_LIMIT";
	public static final String MESH_GRAPH_TX_COMMIT_TIMEOUT_ENV = "MESH_GRAPH_TX_COMMIT_TIMEOUT";
	public static final String MESH_GRAPH_CLUSTER_JOIN_TIMEOUT_ENV = "MESH_GRAPH_CLUSTER_JOIN_TIMEOUT";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database data directory.")
	@EnvironmentVariable(name = MESH_GRAPH_DB_DIRECTORY_ENV, description = "Override the graph database storage directory.")
	private String directory = DEFAULT_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database backup directory.")
	@EnvironmentVariable(name = MESH_GRAPH_BACKUP_DIRECTORY_ENV, description = "Override the graph database backup directory.")
	private String backupDirectory = DEFAULT_BACKUP_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the graph database export directory.")
	@EnvironmentVariable(name = MESH_GRAPH_EXPORT_DIRECTORY_ENV, description = "Override the graph database export directory.")
	private String exportDirectory = DEFAULT_EXPORT_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the graph database admin web server should be started. Default: " + DEFAULT_START_SERVER)
	@EnvironmentVariable(name = MESH_GRAPH_STARTSERVER_ENV, description = "Override the graph database server flag.")
	private Boolean startServer = DEFAULT_START_SERVER;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which controls whether writes to the graph database should be synchronized. Default: " + DEFAULT_SYNC_WRITES)
	@EnvironmentVariable(name = MESH_GRAPH_SYNC_WRITES_ENV, description = "Override the graph database sync writes flag.")
	private boolean synchronizeWrites = DEFAULT_SYNC_WRITES;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Set the timeout in milliseconds for the sync write lock. Default: " + DEFAULT_SYNC_WRITES_TIMEOUT)
	@EnvironmentVariable(name = MESH_GRAPH_SYNC_WRITES_TIMEOUT_ENV, description = "Override the graph database sync write timeout.")
	private long synchronizeWritesTimeout = DEFAULT_SYNC_WRITES_TIMEOUT;

	@JsonProperty(defaultValue = DEFAULT_TX_RETRY_DELAY + "ms")
	@JsonPropertyDescription("The delay in milliseconds when a transaction has to be retried.")
	@EnvironmentVariable(name = MESH_GRAPH_TX_RETRY_DELAY_ENV, description = "Override the transaction retry delay. Default: "
		+ DEFAULT_TX_RETRY_DELAY)
	private int txRetryDelay = DEFAULT_TX_RETRY_DELAY;

	@JsonProperty(defaultValue = DEFAULT_TX_RETRY_LIMIT + " times")
	@JsonPropertyDescription("The limit for the tx retires.")
	@EnvironmentVariable(name = MESH_GRAPH_TX_RETRY_LIMIT_ENV, description = "Override the transaction retry limit. Default: "
		+ DEFAULT_TX_RETRY_LIMIT)
	private int txRetryLimit = DEFAULT_TX_RETRY_LIMIT;

	@JsonProperty(defaultValue = DEFAULT_TX_COMMIT_TIMEOUT + " ms")
	@JsonPropertyDescription("The transaction commit timeout in milliseconds. A timeout value of zero means that transaction commit operations will never timeout.")
	@EnvironmentVariable(name = MESH_GRAPH_TX_COMMIT_TIMEOUT_ENV, description = "Override the transaction commit timeout. Default: "
		+ DEFAULT_TX_COMMIT_TIMEOUT)
	private long txCommitTimeout = DEFAULT_TX_COMMIT_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional set of graph database parameters.")
	private Map<String, String> parameters = new HashMap<>();

	@JsonProperty(defaultValue = DEFAULT_CLUSTER_JOIN_TIMEOUT + " ms")
	@JsonPropertyDescription("The timeout for joining the graphdb cluster in milliseconds. This also includes the time it takes to synchronize the graphdb over the network.")
	@EnvironmentVariable(name = MESH_GRAPH_CLUSTER_JOIN_TIMEOUT_ENV, description = "Override the graphdb cluster join timeout. Default: " + DEFAULT_CLUSTER_JOIN_TIMEOUT + " ms")
	private int clusterJoinTimeout = DEFAULT_CLUSTER_JOIN_TIMEOUT;

	@JsonProperty("diskQuota")
	@JsonPropertyDescription("Options for the disk quota check")
	private DiskQuotaOptions diskQuotaOptions = new DiskQuotaOptions();

	public String getDirectory() {
		return directory;
	}

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

	public String getBackupDirectory() {
		return backupDirectory;
	}

	public GraphStorageOptions setBackupDirectory(String backupDirectory) {
		this.backupDirectory = backupDirectory;
		return this;
	}

	public String getExportDirectory() {
		return exportDirectory;
	}

	public GraphStorageOptions setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
		return this;
	}

	public Boolean getStartServer() {
		return startServer;
	}

	public GraphStorageOptions setStartServer(Boolean startServer) {
		this.startServer = startServer;
		return this;
	}

	public boolean isSynchronizeWrites() {
		return synchronizeWrites;
	}

	public GraphStorageOptions setSynchronizeWrites(boolean synchronizeWrites) {
		this.synchronizeWrites = synchronizeWrites;
		return this;
	}

	public int getTxRetryDelay() {
		return txRetryDelay;
	}

	public GraphStorageOptions setTxRetryDelay(int txRetryDelay) {
		this.txRetryDelay = txRetryDelay;

		return this;
	}

	public long getSynchronizeWritesTimeout() {
		return synchronizeWritesTimeout;
	}

	public GraphStorageOptions setSynchronizeWritesTimeout(long synchronizeWritesTimeout) {
		this.synchronizeWritesTimeout = synchronizeWritesTimeout;
		return this;
	}

	public int getTxRetryLimit() {
		return txRetryLimit;
	}

	public GraphStorageOptions setTxRetryLimit(int txRetryLimit) {
		this.txRetryLimit = txRetryLimit;
		return this;
	}

	public long getTxCommitTimeout() {
		return this.txCommitTimeout;
	}

	public GraphStorageOptions setTxCommitTimeout(long txCommitTimeout) {
		this.txCommitTimeout = txCommitTimeout;
		return this;
	}

	/**
	 * Get the cluster join timeout in ms
	 * @return cluster join timeout
	 */
	public int getClusterJoinTimeout() {
		return clusterJoinTimeout;
	}

	/**
	 * Set the cluster join timeout in ms
	 * @param clusterJoinTimeout timeout in ms
	 * @return fluent API
	 */
	public GraphStorageOptions setClusterJoinTimeout(int clusterJoinTimeout) {
		this.clusterJoinTimeout = clusterJoinTimeout;
		return this;
	}

	/**
	 * Disk quota options
	 * @return options
	 */
	public DiskQuotaOptions getDiskQuotaOptions() {
		return diskQuotaOptions;
	}

	/**
	 * Set the disk quota options
	 * @param diskQuotaOptions options (if null, the options are reset to the default values)
	 * @return fluent API
	 */
	public GraphStorageOptions setDiskQuotaOptions(DiskQuotaOptions diskQuotaOptions) {
		this.diskQuotaOptions = diskQuotaOptions != null ? diskQuotaOptions : new DiskQuotaOptions();
		return this;
	}

	public void validate(MeshOptions meshOptions) {
		if (getStartServer() && getDirectory() == null) {
			throw new NullPointerException(
				"You have not specified a data directory and enabled the graph server. It is not possible to run Gentics Mesh in memory mode and start the graph server.");
		}
		getDiskQuotaOptions().validate(meshOptions);
	}

}
