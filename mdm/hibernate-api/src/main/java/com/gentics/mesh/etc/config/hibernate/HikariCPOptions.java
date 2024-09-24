package com.gentics.mesh.etc.config.hibernate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Options for the Hikari Connection Pool
 */
@GenerateDocumentation
public class HikariCPOptions implements Option {

	private static final boolean DEFAULT_AUTOCOMMIT = false;
	private static final int DEFAULT_CONNECTION_TIMEOUT = 10_000; // 10 seconds
	private static final int DEFAULT_IDLE_TIMEOUT = 600_000; // 10 minutes
	private static final int DEFAULT_MAX_LIFETIME = 1_800_000; // 30 minutes
	private static final int DEFAULT_MIN_IDLE_CONNECTION = 20;
	private static final int DEFAULT_MAX_POOL_SIZE = 20;
	private static final String DEFAULT_POOL_NAME = "MeshHikariCP";
	private static final boolean DEFAULT_REGISTER_MBEANS = false;
	private static final Integer DEFAULT_TRANSACTION_ISOLATION_LEVEL = null; // hikari will use the jdbc driver default isolation level
	private static final int DEFAULT_LEAK_DETECTION_THRESHOLD = 0;

	private static final String HIKARI_CP_AUTOCOMMIT = "HIKARI_CP_AUTOCOMMIT";
	private static final String HIKARI_CP_CONNECTION_TIMEOUT = "HIKARI_CP_CONNECTION_TIMEOUT";
	private static final String HIKARI_CP_IDLE_TIMEOUT = "HIKARI_CP_IDLE_TIMEOUT";
	private static final String HIKARI_CP_MAX_LIFETIME = "HIKARI_CP_MAX_LIFETIME";
	private static final String HIKARI_CP_MIN_IDLE_CONNECTION = "HIKARI_CP_MIN_IDLE_CONNECTION";
	private static final String HIKARI_CP_MAX_POOL_SIZE = "HIKARI_CP_MAX_POOL_SIZE";
	private static final String HIKARI_CP_POOL_NAME = "HIKARI_CP_POOL_NAME";
	private static final String HIKARI_CP_REGISTER_MBEANS = "HIKARI_CP_REGISTER_MBEANS";
	private static final String HIKARI_CP_TRANSACTION_ISOLATION_LEVEL = "HIKARI_CP_TRANSACTION_ISOLATION_LEVEL";
	private static final String HIKARI_CP_LEAK_DETECTION_THRESHOLD = "HIKARI_CP_LEAK_DETECTION_THRESHOLD";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Connection auto-commit behavior.")
	@EnvironmentVariable(name = HIKARI_CP_AUTOCOMMIT, description = "This property controls the default auto-commit behavior of connections returned from the pool. It is a boolean value.")
	private boolean autocommit = DEFAULT_AUTOCOMMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Connection timeout.")
	@EnvironmentVariable(name = HIKARI_CP_CONNECTION_TIMEOUT, description = "This property controls the maximum number of milliseconds that a client (that's you) will wait for a connection from the pool. If this time is exceeded without a connection becoming available, a SQLException will be thrown. Lowest acceptable connection timeout is 250 ms.")
	private Integer connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Idle timeout.")
	@EnvironmentVariable(name = HIKARI_CP_IDLE_TIMEOUT, description = "This property controls the maximum amount of time that a connection is allowed to sit idle in the pool. This setting only applies when minimumIdle is defined to be less than maximumPoolSize. Idle connections will not be retired once the pool reaches minimumIdle connections. Whether a connection is retired as idle or not is subject to a maximum variation of +30 seconds, and average variation of +15 seconds. A connection will never be retired as idle before this timeout. A value of 0 means that idle connections are never removed from the pool. The minimum allowed value is 10000ms (10 seconds).")
	private Integer idleTimeout = DEFAULT_IDLE_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Max lifetime.")
	@EnvironmentVariable(name = HIKARI_CP_MAX_LIFETIME, description = "This property controls the maximum lifetime of a connection in the pool. An in-use connection will never be retired, only when it is closed will it then be removed. On a connection-by-connection basis, minor negative attenuation is applied to avoid mass-extinction in the pool. We strongly recommend setting this value, and it should be several seconds shorter than any database or infrastructure imposed connection time limit. A value of 0 indicates no maximum lifetime (infinite lifetime), subject of course to the idleTimeout setting. The minimum allowed value is 30000ms (30 seconds).")
	private Integer maxLifetime = DEFAULT_MAX_LIFETIME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Minimum idle connections in the pool.")
	@EnvironmentVariable(name = HIKARI_CP_MIN_IDLE_CONNECTION, description = "This property controls the minimum number of idle connections that HikariCP tries to maintain in the pool. If the idle connections dip below this value and total connections in the pool are less than maximumPoolSize, HikariCP will make a best effort to add additional connections quickly and efficiently. However, for maximum performance and responsiveness to spike demands, we recommend not setting this value and instead allowing HikariCP to act as a fixed size connection pool.")
	private Integer minimumIdleConnection = DEFAULT_MIN_IDLE_CONNECTION;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Maximum pool size.")
	@EnvironmentVariable(name = HIKARI_CP_MAX_POOL_SIZE, description = "This property controls the maximum size that the pool is allowed to reach, including both idle and in-use connections. Basically this value will determine the maximum number of actual connections to the database backend. A reasonable value for this is best determined by your execution environment. When the pool reaches this size, and no idle connections are available, calls to getConnection() will block for up to connectionTimeout milliseconds before timing out.")
	private Integer maxPoolSize = DEFAULT_MAX_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the connection pool.")
	@EnvironmentVariable(name = HIKARI_CP_POOL_NAME, description = "This property represents a user-defined name for the connection pool and appears mainly in logging and JMX management consoles to identify pools and pool configurations.")
	private String poolName = DEFAULT_POOL_NAME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Register JMX Management Beans.")
	@EnvironmentVariable(name = HIKARI_CP_REGISTER_MBEANS, description = "This property controls whether or not JMX Management Beans (\"MBeans\") are registered or not.")
	private Boolean registerMBeans = DEFAULT_REGISTER_MBEANS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Transaction isolation level.")
	@EnvironmentVariable(name = HIKARI_CP_TRANSACTION_ISOLATION_LEVEL, description = "This property controls the default transaction isolation level of connections returned from the pool. If this property is not specified, the default transaction isolation level defined by the JDBC driver is used. Only use this property if you have specific isolation requirements that are common for all queries. The value of this property is the constant name from the Connection class such as TRANSACTION_READ_COMMITTED, TRANSACTION_REPEATABLE_READ. Setting it to null defaults to isolation level defined in the JDBC driver.")
	private Integer transactionIsolationLevel = DEFAULT_TRANSACTION_ISOLATION_LEVEL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Leak detection threshold.")
	@EnvironmentVariable(name = HIKARI_CP_LEAK_DETECTION_THRESHOLD, description = "This property controls the amount of time that a connection can be out of the pool before a message is logged indicating a possible connection leak. A value of 0 means leak detection is disabled. Lowest acceptable value for enabling leak detection is 2000 (2 seconds).")
	private Integer leakDetectionThreshold = DEFAULT_LEAK_DETECTION_THRESHOLD;

	public boolean isAutocommit() {
		return autocommit;
	}

	public void setAutocommit(boolean autocommit) {
		this.autocommit = autocommit;
	}

	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Integer getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(Integer idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public Integer getMaxLifetime() {
		return maxLifetime;
	}

	public void setMaxLifetime(Integer maxLifetime) {
		this.maxLifetime = maxLifetime;
	}

	public Integer getMinimumIdleConnection() {
		return minimumIdleConnection;
	}

	public void setMinimumIdleConnection(Integer minimumIdleConnection) {
		this.minimumIdleConnection = minimumIdleConnection;
	}

	public Integer getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(Integer maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public Boolean getRegisterMBeans() {
		return registerMBeans;
	}

	public void setRegisterMBeans(Boolean registerMBeans) {
		this.registerMBeans = registerMBeans;
	}

	public Integer getTransactionIsolationLevel() {
		return transactionIsolationLevel;
	}

	public void setTransactionIsolationLevel(Integer transactionIsolationLevel) {
		this.transactionIsolationLevel = transactionIsolationLevel;
	}

	public Integer getLeakDetectionThreshold() {
		return leakDetectionThreshold;
	}

	public void setLeakDetectionThreshold(Integer leakDetectionThreshold) {
		this.leakDetectionThreshold = leakDetectionThreshold;
	}
}
