package com.gentics.mesh.changelog;

import com.gentics.mesh.OptionsLoader;
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

	public static void main(String[] args) {
		new ChangelogRunner().run();
	}

	/**
	 * Start dedicated changelog runner which executes the stored changelog.
	 */
	public void run() {
		run(OptionsLoader.createOrloadOptions());
	}

	/**
	 * Start dedicated changelog runner which executes the stored changelog.
	 * 
	 * @param options
	 */
	public void run(MeshOptions options) {
		log.info("Starting changelog runner");
//		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ChangelogSpringConfiguration.class)) {
//			ctx.start();
			try {

				ChangelogSystem cls = new ChangelogSystem(getDatabase(options));
				if (!cls.applyChanges()) {
					log.error("The changelog could not be applied successfully. See log above.");
					System.exit(10);
				}
			} catch (Exception e) {
				log.error(e);
			}
//		}
		log.info("Terminating changelog runner");
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
