package com.gentics.mesh.liquibase;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Class used for liquibase interaction
 */
@Singleton
public class LiquibaseConnectionProvider {

	/**
	 * This is where we expect the Liquibase changelog root to exist. Usually placed into the database connector implementation jar.
	 */
	public static final String changelog = "META-INF/liquibase/changelog-master.xml";

	private final DatabaseConnector databaseConnector;

	@Inject
	public LiquibaseConnectionProvider(HibernateMeshOptions options, DatabaseConnector connector) {
		// By requiring DatabaseConnector we ensure that it exists in the class loader, and we can use its metadata.
		// It is used for the Liquibase changelog mechanism.
		databaseConnector = connector;
	}

	/**
	 * provides an instance of {@link Liquibase}
	 * */
	public LiquibaseStartupContext getLiquibase(Connection connection) throws DatabaseException {
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
		Liquibase lb = new Liquibase(changelog, new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),  database);
		return new LiquibaseStartupContext(lb, databaseConnector);
	}
}
