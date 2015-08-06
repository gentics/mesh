package com.gentics.mesh.graphdb;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.gentics.mesh.graphdb.spi.Database;

public class DatabaseService {

	private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
	private static DatabaseService service;
	private ServiceLoader<Database> loader;

	private DatabaseService() {
		loader = ServiceLoader.load(Database.class);
	}

	public static synchronized DatabaseService getInstance() {
		if (service == null) {
			service = new DatabaseService();
		}
		return service;
	}

	public Database getDatabase() {
		Database database = null;
		try {
			Iterator<Database> databaseProviders = loader.iterator();
			while (database == null && databaseProviders.hasNext()) {
				database = databaseProviders.next();
				log.debug("Found database service provider {" + database.getClass() + "}");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		return database;
	}

}
