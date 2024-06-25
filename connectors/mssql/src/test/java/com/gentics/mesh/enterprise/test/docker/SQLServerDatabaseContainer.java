package com.gentics.mesh.enterprise.test.docker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Database Container for MS SQL
 */
public class SQLServerDatabaseContainer extends PreparingDatabaseContainer<SQLServerDatabaseContainer> {
	private static final int DEFAULT_PORT = 1433;

	/**
	 * Create an instance
	 */
	public SQLServerDatabaseContainer() {
		super("mcr.microsoft.com/mssql/server:2022-latest");
	}

	@Override
	protected void configure() {
		addEnv("ACCEPT_EULA", "Y");
		addEnv("SA_PASSWORD", DEFAULT_PASSWORD);
		super.configure();		
		waitingFor(Wait.forLogMessage(".*SQL Server is now ready for client connections.*\\n", 1).withStartupTimeout(Duration.ofMinutes(2)));
	}

	@Override
	public int getDatabasePort() {
		return DEFAULT_PORT;
	}

	@Override
	protected Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlserver://localhost:" + getMappedPort() + ";encrypt=false", "SA",
				DEFAULT_PASSWORD);
	}

	@Override
	protected Connection getConnection(String name) throws SQLException {
		return DriverManager.getConnection(
				"jdbc:sqlserver://localhost:" + getMappedPort() + ";encrypt=false;databaseName=" + name, "SA",
				DEFAULT_PASSWORD);
	}

	@Override
	protected void createDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("CREATE DATABASE " + name + " COLLATE Latin1_General_100_CS_AS_KS_WS")) {
			pst.executeUpdate();
		}
		try (PreparedStatement pst = c.prepareStatement("ALTER DATABASE " + name + " SET READ_COMMITTED_SNAPSHOT ON")) {
			pst.executeUpdate();
		}
		try (PreparedStatement pst = c.prepareStatement("sp_configure 'show advanced options', 1")) {
			pst.execute();
			try (PreparedStatement pst1 = c.prepareStatement("reconfigure")) {
				pst1.execute();
				try (PreparedStatement pst2 = c.prepareStatement("sp_configure 'max server memory', 1596")) {
					pst2.execute();
					try (PreparedStatement pst3 = c.prepareStatement("reconfigure")) {
						pst3.execute();
					}
				}
			}
		}
	}

	@Override
	protected void dropDatabase(Connection c, String name) throws SQLException {
		try (PreparedStatement pst = c.prepareStatement("DROP DATABASE " + name)) {
			pst.executeUpdate();
		}
	}

}
