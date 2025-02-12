package com.gentics.mesh.liquibase;

import com.gentics.mesh.database.connector.DatabaseConnector;

import liquibase.Liquibase;

/**
 * A wrapper around {@link Liquibase} holding extra context.
 */
public class LiquibaseStartupContext implements AutoCloseable {

	private static DatabaseConnector CONNECTOR;

	private final Liquibase liquibase;

	/**
	 * Limit ctor access by this package.
	 * 
	 * @param liquibase
	 * @param databaseConnector
	 */
	LiquibaseStartupContext(Liquibase liquibase, DatabaseConnector databaseConnector) {
		this.liquibase = liquibase;
		synchronized (LiquibaseStartupContext.class) {
			CONNECTOR = databaseConnector;
		}
	}

	@Override
	public void close() throws Exception {
		synchronized (LiquibaseStartupContext.class) {
			CONNECTOR = null;
		}
	}

	/**
	 * Extract the liquibase.
	 * 
	 * @return
	 */
	public Liquibase liquibase() {
		return liquibase;
	}

	/**
	 * Get the {@link DatabaseConnector} used during the Liquibase changelog application. Throws an {@link IllegalStateException} if called arbitrary.
	 * 
	 * @return
	 */
	public static DatabaseConnector getConnectorIfStartup() {
		if (CONNECTOR != null) {
			return CONNECTOR;
		} else {
			throw new IllegalStateException("Connector is requested out of the Liquibase startup scope");
		}
	}
}
