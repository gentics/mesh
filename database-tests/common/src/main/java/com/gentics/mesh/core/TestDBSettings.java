package com.gentics.mesh.core;

/**
 * POJO for the test DB settings sent from the test DB manager
 */
public class TestDBSettings {
	protected String host;

	protected int port;

	protected String database;

	protected String schema;

	protected String username;

	protected String password;

	/**
	 * DB host
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host
	 * @param host host
	 * @return fluent API
	 */
	public TestDBSettings setHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * DB Port
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port
	 * @param port port
	 * @return fluent API
	 */
	public TestDBSettings setPort(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Database name
	 * @return database name
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * Set the database name
	 * @param database database name
	 * @return fluent API
	 */
	public TestDBSettings setDatabase(String database) {
		this.database = database;
		return this;
	}

	/**
	 * Schema name
	 * @return schema name
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Set the schema name
	 * @param schema schema name
	 * @return fluent API
	 */
	public TestDBSettings setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	/**
	 * Username
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username
	 * @param username username
	 * @return fluent API
	 */
	public TestDBSettings setUsername(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Password
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password
	 * @param password password
	 * @return fluent API
	 */
	public TestDBSettings setPassword(String password) {
		this.password = password;
		return this;
	}
}
