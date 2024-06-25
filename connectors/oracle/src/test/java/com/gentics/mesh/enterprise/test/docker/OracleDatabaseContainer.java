package com.gentics.mesh.enterprise.test.docker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Database Container for Oracle
 */
public class OracleDatabaseContainer extends PreparingDatabaseContainer<OracleDatabaseContainer> {
	private static final int DEFAULT_PORT = 1521;

	/**
	 * Create an instance
	 */
	public OracleDatabaseContainer() {
		super("gvenzl/oracle-xe:latest");
	}

	@Override
	protected void configure() {
		super.configure();
		addEnv("ORACLE_DATABASE", DEFAULT_DATABASE);
		addEnv("ORACLE_PASSWORD", DEFAULT_PASSWORD);
		addEnv("APP_USER", DEFAULT_USERNAME);
		addEnv("APP_USER_PASSWORD", DEFAULT_PASSWORD);
		waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!*\\n", 1).withStartupTimeout(Duration.ofMinutes(2)));
	}

	@Override
	public int getDatabasePort() {
		return DEFAULT_PORT;
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		// connect to oracle and init
		try (Connection c = DriverManager.getConnection(
				"jdbc:oracle:thin:@localhost:" + getMappedPort() + "/" + DEFAULT_DATABASE, "system", DEFAULT_PASSWORD)) {
			try (Statement st = c.createStatement()) {
				st.executeUpdate("ALTER SYSTEM SET OPEN_CURSORS=4444 SID='*' SCOPE=BOTH");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		super.containerIsStarted(containerInfo);
	}

	@Override
	protected Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:oracle:thin:@localhost:" + getMappedPort() + "/" + DEFAULT_DATABASE,
				"system", DEFAULT_PASSWORD);
	}

	@Override
	protected Connection getConnection(String name) throws SQLException {
		return DriverManager.getConnection("jdbc:oracle:thin:@localhost:" + getMappedPort() + "/" + DEFAULT_DATABASE,
				name, DEFAULT_PASSWORD);
	}

	@Override
	protected void createDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c
				.prepareStatement("CREATE USER " + name + " IDENTIFIED BY \"" + DEFAULT_PASSWORD + "\"")) {
			pst.executeUpdate();
		}
		try (PreparedStatement pst = c.prepareStatement("GRANT ALL PRIVILEGES TO " + name)) {
			pst.executeUpdate();
		}
	}

	@Override
	protected void dropDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("DROP USER " + name + " CASCADE")) {
			pst.executeUpdate();
		}
	}
}
