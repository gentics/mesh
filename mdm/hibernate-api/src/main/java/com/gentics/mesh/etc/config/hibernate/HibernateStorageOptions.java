package com.gentics.mesh.etc.config.hibernate;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.NativeQueryFiltering;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Hibernate-based storage options
 * 
 * @author plyhun
 */
@GenerateDocumentation
public class HibernateStorageOptions implements Option {

	public static final String SQL_PARAMETERS_LIMIT_OPTION_DB_DEFINED = "definedByDatabase";
	public static final String SQL_PARAMETERS_LIMIT_OPTION_UNLIMITED = "unlimited";

	public static final String DEFAULT_DATA_ROOT = "data";
	public static final String DEFAULT_DATABASE = "mesh";

	public static final int DEFAULT_TX_RETRY_LIMIT = 10;
	public static final int DEFAULT_TX_RETRY_DELAY_MILLIS = 2000;
	public static final boolean DEFAULT_SYNC_WRITES = false;
	public static final long DEFAULT_SYNC_WRITES_TIMEOUT = 60_000;
	public static final long DEFAULT_SLOW_SQL_THRESHOLD = 60_000;

	public static final String DEFAULT_CONNECTION_USERNAME = "admin";
	public static final String DEFAULT_CONNECTION_PASSWORD = "admin";
	public static final Boolean DEFAULT_SHOW_SQL = false;
	public static final Boolean DEFAULT_FORMAT_SQL = false;
	public static final Boolean DEFAULT_SECOND_LEVEL_CACHE_ENABLED = true;
	public static final Boolean DEFAULT_GENERATE_STATISTICS = false;
	public static final long DEFAULT_QUERY_TIMEOUT = 0;
	public static final int DEFAULT_HIBERNATE_JDBC_BATCH_SIZE = 5;
	public static final String DEFAULT_SQL_PARAMETERS_LIMIT = SQL_PARAMETERS_LIMIT_OPTION_DB_DEFINED;
	public static final String DEFAULT_EXPORT_DIRECTORY = DEFAULT_DATA_ROOT + File.separator + "export";
	public static final long DEFAULT_STALE_TX_CHECK_INTERVAL_MS = 10_000;

	public static final String MESH_JDBC_DRIVER_CLASS = "MESH_JDBC_DRIVER_CLASS";
	public static final String MESH_DATABASE_ADDRESS = "MESH_DATABASE_ADDRESS";
	public static final String MESH_JDBC_CONNECTION_URL_EXTRA_PARAMS = "MESH_JDBC_CONNECTION_URL_EXTRA_PARAMS";
	public static final String MESH_JDBC_DATABASE_NAME = "MESH_JDBC_DATABASE_NAME";
	public static final String MESH_JDBC_DIALECT_CLASS = "MESH_JDBC_DIALECT_CLASS";

	public static final String MESH_TX_RETRY_LIMIT = "MESH_JDBC_RETRY_LIMIT";
	public static final String MESH_TX_RETRY_DELAY_MILLIS = "MESH_TX_RETRY_DELAY_SECONDS";
	public static final String MESH_DB_SYNC_WRITES_ENV = "MESH_DB_SYNC_WRITES";
	public static final String MESH_DB_SYNC_WRITES_TIMEOUT_ENV = "MESH_DB_SYNC_WRITES_TIMEOUT";
	public static final String MESH_DB_SQL_PARAMETERS_LIMIT_ENV = "MESH_DB_SQL_PARAMETERS_LIMIT";
	public static final String MESH_DB_CONNECTOR_CLASSPATH = "MESH_DB_CONNECTOR_CLASSPATH";
	public static final NativeQueryFiltering DEFAULT_NATIVE_QUERY_FILTERING = NativeQueryFiltering.ON_DEMAND;

	public static final String MESH_JDBC_CONNECTION_USERNAME = "MESH_JDBC_CONNECTION_USERNAME";
	public static final String MESH_JDBC_CONNECTION_PASSWORD = "MESH_JDBC_CONNECTION_PASSWORD";
	public static final String MESH_HIBERNATE_SHOW_SQL = "MESH_HIBERNATE_SHOW_SQL";
	public static final String MESH_HIBERNATE_FORMAT_SQL = "MESH_HIBERNATE_FORMAT_SQL";
	public static final String MESH_HIBERNATE_SLOW_SQL_THRESHOLD = "MESH_HIBERNATE_SLOW_SQL_THRESHOLD";
	public static final String MESH_HIBERNATE_USE_CLOB_FOR_STRINGS = "MESH_HIBERNATE_USE_CLOB_FOR_STRINGS";
	public static final String MESH_HIBERNATE_SECOND_LEVEL_CACHE_ENABLED = "MESH_HIBERNATE_SECOND_LEVEL_CACHE_ENABLED";
	public static final String MESH_HIBERNATE_GENERATE_STATISTICS = "MESH_HIBERNATE_GENERATE_STATISTICS";
	public static final String MESH_HIBERNATE_QUERY_TIMEOUT = "MESH_HIBERNATE_QUERY_TIMEOUT";
	public static final String MESH_HIBERNATE_JDBC_BATCH_SIZE = "MESH_HIBERNATE_JDBC_BATCH_SIZE";
	public static final String MESH_STALE_TX_CHECK_INTERVAL = "MESH_STALE_TX_CHECK_INTERVAL";
	public static final String MESH_HIBERNATE_NATIVE_QUERY_FILTERING = "MESH_HIBERNATE_NATIVE_QUERY_FILTERING";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enables the native database level filtering for queries. Default: ON_DEMAND")
	@EnvironmentVariable(name = MESH_HIBERNATE_NATIVE_QUERY_FILTERING, description = "Override the configured native query filtering.")
	private NativeQueryFiltering nativeQueryFiltering = DEFAULT_NATIVE_QUERY_FILTERING;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Transaction retry count.")
	@EnvironmentVariable(name = MESH_TX_RETRY_LIMIT, description = "Override the default transaction retry count.")
	private int retryLimit = DEFAULT_TX_RETRY_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Transaction wait interval between retries, in milliseconds.")
	@EnvironmentVariable(name = MESH_TX_RETRY_DELAY_MILLIS, description = "Override the default transaction retry delay interval.")
	private int retryDelayMillis = DEFAULT_TX_RETRY_DELAY_MILLIS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional JDBC driver class name. Use this to override the default database connector provided driver class.")
	@EnvironmentVariable(name = MESH_JDBC_DRIVER_CLASS, description = "Override the default JDBC driver class.")
	private String driverClass;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional Hibernate dialect class name. User this to override the default database connector provided dialect class.")
	@EnvironmentVariable(name = MESH_JDBC_DIALECT_CLASS, description = "Override the default JDBC dialect class.")
	private String dialectClass;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("Database address in HOST<:PORT> format.")
	@EnvironmentVariable(name = MESH_DATABASE_ADDRESS, description = "Override the database address.")
	private String databaseAddress;

	@JsonProperty(required = false)
	@JsonPropertyDescription("JDBC connection URL extra parameters.")
	@EnvironmentVariable(name = MESH_JDBC_CONNECTION_URL_EXTRA_PARAMS, description = "Add the default JDBC connection URL extra params.")
	private String connectionUrlExtraParams = StringUtils.EMPTY;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("Database name.")
	@EnvironmentVariable(name = MESH_JDBC_DATABASE_NAME, description = "Override the default database name.")
	private String databaseName = DEFAULT_DATABASE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Mesh Database Connector classpath. This can be a path to either a connector JAR file, or a folder structure containing all the necessary JARs. Symlinks are supported.")
	@EnvironmentVariable(name = MESH_DB_CONNECTOR_CLASSPATH, description = "Set the Mesh Database Connector classpath.")
	private String databaseConnectorClasspath = StringUtils.EMPTY;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("JDBC connection username.")
	@EnvironmentVariable(name = MESH_JDBC_CONNECTION_USERNAME, description = "Set the JDBC connection username.")
	private String connectionUsername = DEFAULT_CONNECTION_USERNAME;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("JDBC connection password.")
	@EnvironmentVariable(name = MESH_JDBC_CONNECTION_PASSWORD, description = "Set the JDBC connection password.", isSensitive = true)
	private String connectionPassword = DEFAULT_CONNECTION_PASSWORD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Are actual SQL queries logged?")
	@EnvironmentVariable(name = MESH_HIBERNATE_SHOW_SQL, description = "Set whether the driver-specific SQL queries should be logged.")
	private boolean showSql = DEFAULT_SHOW_SQL;
	
	@JsonProperty(required = false)
	@JsonPropertyDescription("Are actual SQL queries formatted during logging?")
	@EnvironmentVariable(name = MESH_HIBERNATE_FORMAT_SQL, description = "Set whether the driver-specific SQL queries should be formatted while logged.")
	private boolean formatSql = DEFAULT_FORMAT_SQL;

	@JsonProperty(required = true)
	@JsonPropertyDescription("HikariCP (connection pool) options.")
	private HikariCPOptions hikariOptions = new HikariCPOptions();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which controls whether writes to the database should be synchronized. Setting this flag improves the stability of massive database updates, by the cost of overall performance. Default: " + DEFAULT_SYNC_WRITES)
	@EnvironmentVariable(name = MESH_DB_SYNC_WRITES_ENV, description = "Override the database sync writes flag.")
	private boolean synchronizeWrites = DEFAULT_SYNC_WRITES;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the timeout in milliseconds for the sync write lock. Default: " + DEFAULT_SYNC_WRITES_TIMEOUT)
	@EnvironmentVariable(name = MESH_DB_SYNC_WRITES_TIMEOUT_ENV, description = "Override the database sync write timeout.")
	private long synchronizeWritesTimeout = DEFAULT_SYNC_WRITES_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enable Hibernate second-level cache")
	@EnvironmentVariable(name = MESH_HIBERNATE_SECOND_LEVEL_CACHE_ENABLED, description = "Set whether second-level cache should be enabled.")
	private boolean secondLevelCacheEnabled = DEFAULT_SECOND_LEVEL_CACHE_ENABLED;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enable generation of hibernate statistics")
	@EnvironmentVariable(name =  MESH_HIBERNATE_GENERATE_STATISTICS, description = "Set whether hibernate should generate statistics.")
	private boolean generateStatistics = DEFAULT_GENERATE_STATISTICS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Global query timeout limit in milliseconds. A timeout value of zero means there is no query timeout.")
	@EnvironmentVariable(name = MESH_HIBERNATE_QUERY_TIMEOUT, description = "Query timeout for hibernate. Default: " + DEFAULT_QUERY_TIMEOUT)
	private long queryTimeout = DEFAULT_QUERY_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Define a threshold in milliseconds, upon which a SQL query is considered slow and produces a log warning.")
	@EnvironmentVariable(name = MESH_HIBERNATE_SLOW_SQL_THRESHOLD, description = "Slow SQL query threshold in milliseconds. Default: " + DEFAULT_SLOW_SQL_THRESHOLD)
	private long slowSqlThreshold = DEFAULT_SLOW_SQL_THRESHOLD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Hibernate JDBC Batch Size. Recommended values are between 5 and 30")
	@EnvironmentVariable(name = MESH_HIBERNATE_JDBC_BATCH_SIZE, description = "Set batch size. Recommended values are between 5 and 30")
	private long jdbcBatchSize = DEFAULT_HIBERNATE_JDBC_BATCH_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("SQL parameter number limit.")
	@EnvironmentVariable(name = MESH_DB_SQL_PARAMETERS_LIMIT_ENV, description = "Limit the number of SQL parameters in selected lookup queries. Can be either `" 
							+ SQL_PARAMETERS_LIMIT_OPTION_DB_DEFINED + "` or `" + SQL_PARAMETERS_LIMIT_OPTION_UNLIMITED + "` or a custom number (greater than zero). Default is: " + DEFAULT_SQL_PARAMETERS_LIMIT)
	private String sqlParametersLimit = DEFAULT_SQL_PARAMETERS_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Interval in ms for periodic check for stale transactions. Defaults to " + DEFAULT_STALE_TX_CHECK_INTERVAL_MS + " ms.")
	@EnvironmentVariable(name = MESH_STALE_TX_CHECK_INTERVAL, description = "Overwrite the interval (in ms) for periodic check for stale transactions.")
	private long staleTxCheckInterval = DEFAULT_STALE_TX_CHECK_INTERVAL_MS;

	public String getConnectionUsername() {
		return connectionUsername;
	}

	@Setter
	public HibernateStorageOptions setConnectionUsername(String connectionUsername) {
		this.connectionUsername = connectionUsername;
		return this;
	}

	public String getConnectionPassword() {
		return connectionPassword;
	}

	@Setter
	public HibernateStorageOptions setConnectionPassword(String connectionPassword) {
		this.connectionPassword = connectionPassword;
		return this;
	}

	public boolean isShowSql() {
		return showSql;
	}

	@Setter
	public HibernateStorageOptions setShowSql(boolean showSql) {
		this.showSql = showSql;
		return this;
	}

	public boolean isFormatSql() {
		return formatSql;
	}

	@Setter
	public HibernateStorageOptions setFormatSql(boolean formatSql) {
		this.formatSql = formatSql;
		return this;
	}

	public String getDriverClass() {
		return driverClass;
	}

	@Setter
	public HibernateStorageOptions setDriverClass(String driverClass) {
		this.driverClass = driverClass;
		return this;
	}

	public String getDialectClass() {
		return dialectClass;
	}

	@Setter
	public HibernateStorageOptions setDialectClass(String dialectClass) {
		this.dialectClass = dialectClass;
		return this;
	}

	public String getDatabaseAddress() {
		return databaseAddress;
	}

	@Setter
	public HibernateStorageOptions setDatabaseAddress(String databaseAddress) {
		this.databaseAddress = databaseAddress;
		return this;
	}

	public String getDatabaseConnectorClasspath() {
		return databaseConnectorClasspath;
	}

	@Setter
	public HibernateStorageOptions setDatabaseConnectorClasspath(String driverClasspath) {
		this.databaseConnectorClasspath = driverClasspath;
		return this;
	}
	
	public String getDatabaseName() {
		return databaseName;
	}

	@Setter
	public HibernateStorageOptions setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
		return this;
	}

	public int getRetryLimit() {
		return retryLimit;
	}

	@Setter
	public HibernateStorageOptions setRetryLimit(int retryCount) {
		this.retryLimit = retryCount;
		return this;
	}

	public int getRetryDelayMillis() {
		return retryDelayMillis;
	}

	@Setter
	public HibernateStorageOptions setRetryDelayMillis(int retryWaitMillis) {
		this.retryDelayMillis = retryWaitMillis;
		return this;
	}

	public HikariCPOptions getHikariOptions() {
		return hikariOptions;
	}

	@Setter
	public HibernateStorageOptions setHikariOptions(HikariCPOptions hikariOptions) {
		this.hikariOptions = hikariOptions;
		return this;
	}

	public String getConnectionUrlExtraParams() {
		return connectionUrlExtraParams;
	}

	@Setter
	public HibernateStorageOptions setConnectionUrlExtraParams(String connectionUrlExtraParams) {
		this.connectionUrlExtraParams = connectionUrlExtraParams;
		return this;
	}

	public boolean isSynchronizeWrites() {
		return synchronizeWrites;
	}

	@Setter
	public HibernateStorageOptions setSynchronizeWrites(boolean synchronizeWrites) {
		this.synchronizeWrites = synchronizeWrites;
		return this;
	}

	public long getSynchronizeWritesTimeout() {
		return synchronizeWritesTimeout;
	}

	@Setter
	public HibernateStorageOptions setSynchronizeWritesTimeout(long synchronizeWritesTimeout) {
		this.synchronizeWritesTimeout = synchronizeWritesTimeout;
		return this;
	}

	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	@Setter
	public HibernateStorageOptions setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
		return this;
	}

	public boolean isGenerateStatistics() {
		return generateStatistics;
	}

	@Setter
	public HibernateStorageOptions setGenerateStatistics(boolean generateStatistics) {
		this.generateStatistics = generateStatistics;
		return this;
	}

	public long getQueryTimeout() {
		return queryTimeout;
	}

	@Setter
	public HibernateStorageOptions setQueryTimeout(long queryTimeout) {
		this.queryTimeout = queryTimeout;
		return this;
	}

	public long getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Setter
	public HibernateStorageOptions setJdbcBatchSize(long jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
		return this;
	}

	public String getSqlParametersLimit() {
		return sqlParametersLimit;
	}

	@Setter
	public void setSqlParametersLimit(String sqlParametersLimit) {
		this.sqlParametersLimit = sqlParametersLimit;
	}

	public long getSlowSqlThreshold() {
		return slowSqlThreshold;
	}

	@Setter
	public void setSlowSqlThreshold(long slowSqlThreshold) {
		this.slowSqlThreshold = slowSqlThreshold;
	}

	public NativeQueryFiltering getNativeQueryFiltering() {
		return nativeQueryFiltering;
	}

	@Setter
	public HibernateStorageOptions setNativeQueryFiltering(NativeQueryFiltering nativeQueryFiltering) {
		this.nativeQueryFiltering = nativeQueryFiltering;
		return this;
	}

	public long getStaleTxCheckInterval() {
		return staleTxCheckInterval;
	}

	@Setter
	public HibernateStorageOptions setStaleTxCheckInterval(long staleTxCheckInterval) {
		this.staleTxCheckInterval = staleTxCheckInterval;
		return this;
	}

	/**
	* Validate the nested options
	 */
	@Override
	public void validate(MeshOptions options) {
		if (getHikariOptions() != null) {
			getHikariOptions().validate(options);
		}
		try {
			switch (sqlParametersLimit) {
			case SQL_PARAMETERS_LIMIT_OPTION_DB_DEFINED:
			case SQL_PARAMETERS_LIMIT_OPTION_UNLIMITED:
				break;
			default:
				assert Integer.parseInt(sqlParametersLimit) > 0;
			}
		} catch (Throwable e) {
			throw new IllegalArgumentException("Invalid value for `storageOptions.sqlParametersLimit`", e);
		}
	}
}
