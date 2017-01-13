package com.gentics.mesh.changelog;

import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * The changelog runner is a dedicated tool which can be used to update a mesh database without the need to startup mesh.
 */
public class ChangelogRunner {

	private static final Logger log = LoggerFactory.getLogger(ChangelogRunner.class);

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	/**
	 * Load the graph database which was configured in the mesh storage options.
	 * 
	 * @param options
	 * @return
	 */
	public static Database getDatabase(MeshOptions options) {
		DatabaseService databaseService = DatabaseService.getInstance();
		Database database = databaseService.getDatabase();
		if (database == null) {
			String message = "No database provider could be found.";
			log.error(message);
			throw new RuntimeException(message);
		}
		try {
			GraphStorageOptions graphOptions = options.getStorageOptions();
			database.init(graphOptions, Vertx.vertx());
			return database;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
