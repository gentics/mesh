package com.gentics.mesh.graphdb;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.gentics.mesh.graphdb.spi.LegacyDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * SPI provider for graph databases.
 */
public class DatabaseService {

	private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
	private static DatabaseService service;
	private ServiceLoader<LegacyDatabase> loader;

	private DatabaseService() {
		loader = ServiceLoader.load(LegacyDatabase.class);
	}

	/**
	 * Return the database service instance.
	 * 
	 * @return
	 */
	public static synchronized DatabaseService getInstance() {
		if (service == null) {
			service = new DatabaseService();
		}
		return service;
	}

	/**
	 * Iterate over all providers return the last provider.
	 * 
	 * @return
	 */
	public LegacyDatabase getDatabase() {
		LegacyDatabase database = null;
		//TODO fail when more than one provider was found?
		try {
			Iterator<LegacyDatabase> databaseProviders = loader.iterator();
			while (database == null && databaseProviders.hasNext()) {
				database = databaseProviders.next();
				log.debug("Found database service provider {" + database.getClass() + "}");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		if (database == null) {
			throw new RuntimeException("Could not find database provider.");
		}
		return database;
	}

}
