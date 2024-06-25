package com.gentics.mesh.enterprise.test.docker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Database Container for Postgres
 */
public class PostgresDatabaseContainer extends PreparingDatabaseContainer<PostgresDatabaseContainer> {
	private static final int DEFAULT_PORT = 5432;

	/**
	 * Create an instance
	 */
	public PostgresDatabaseContainer() {
		super("postgres:latest");
	}

	@Override
	protected void configure() {
		addEnv("POSTGRES_DB", DEFAULT_DATABASE);
		addEnv("POSTGRES_USER", DEFAULT_USERNAME);
		addEnv("POSTGRES_PASSWORD", DEFAULT_PASSWORD);
		super.configure();
		waitingFor(Wait.forLogMessage(".*PostgreSQL init process complete; ready for start up.*\\n", 1)
				.withStartupTimeout(Duration.ofMinutes(2)));
	}

	@Override
	public int getDatabasePort() {
		return DEFAULT_PORT;
	}

	@Override
	protected Connection getConnection() throws SQLException {
		return DriverManager.getConnection(
				"jdbc:postgresql://localhost:" + getMappedPort() + "/" + DEFAULT_DATABASE + "?autosave=always",
				DEFAULT_USERNAME, DEFAULT_PASSWORD);
	}

	@Override
	protected Connection getConnection(String name) throws SQLException {
		return DriverManager.getConnection("jdbc:postgresql://localhost:" + getMappedPort() + "/" + DEFAULT_DATABASE
				+ "?autosave=always&currentSchema=" + name, DEFAULT_USERNAME, DEFAULT_PASSWORD);
	}

	@Override
	protected void createDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("CREATE SCHEMA " + name)) {
			pst.executeUpdate();
		}
	}

	@Override
	protected void dropDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("DROP SCHEMA " + name + " CASCADE")) {
			pst.executeUpdate();
		}
	}
}
