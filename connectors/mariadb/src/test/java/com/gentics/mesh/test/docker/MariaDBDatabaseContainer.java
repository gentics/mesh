package com.gentics.mesh.test.docker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Database container for mariadb
 */
public class MariaDBDatabaseContainer extends PreparingDatabaseContainer<MariaDBDatabaseContainer> {
	private static final int DEFAULT_PORT = 3306;

	/**
	 * Create an instance
	 */
	public MariaDBDatabaseContainer() {
		// Note: Version is pinned to 10.7, because "latest" image cannot be used as of the time of this writing.
		// See https://github.com/MariaDB/mariadb-docker/issues/434
		super("mariadb:10.7");
	}

	@Override
	protected void configure() {
		addEnv("MYSQL_DATABASE", DEFAULT_DATABASE);
		addEnv("MYSQL_USER", DEFAULT_USERNAME);
		addEnv("MYSQL_PASSWORD", DEFAULT_PASSWORD);
		addEnv("MYSQL_ROOT_PASSWORD", DEFAULT_PASSWORD);
		setCommand("--transaction-isolation=READ-COMMITTED", "--binlog-format=ROW", "--log-bin=mysqld-bin", "--max-allowed-packet=67108864");
		super.configure();
		waitingFor(Wait
				.forLogMessage(".*socket: '/run/mysqld/mysqld.sock'  port: 3306  mariadb.org binary distribution*\\n",
						1)
				.withStartupTimeout(Duration.ofMinutes(5)));
	}

	@Override
	public int getDatabasePort() {
		return DEFAULT_PORT;
	}

	@Override
	protected Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:mariadb://localhost:" + getMappedPort() + "/", "root", DEFAULT_PASSWORD);
	}

	@Override
	protected Connection getConnection(String name) throws SQLException {
		return DriverManager.getConnection("jdbc:mariadb://localhost:" + getMappedPort() + "/" + name, "root", DEFAULT_PASSWORD);
	}

	@Override
	protected void createDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("CREATE DATABASE " + name + " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;")) {
			pst.executeUpdate();
		}
		try (PreparedStatement pst = c
				.prepareStatement("GRANT ALL PRIVILEGES ON " + name + ".* TO '" + DEFAULT_USERNAME + "'@'%';")) {
			pst.executeUpdate();
		}
	}

	@Override
	protected void dropDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("DROP DATABASE " + name)) {
			pst.executeUpdate();
		}
	}
}
