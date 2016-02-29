package com.gentics.mesh.changelog;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
@Scope(value = "singleton")
public class ChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(ChangelogSystem.class);

	public Database getDatabase() {
		DatabaseService databaseService = DatabaseService.getInstance();
		Database database = databaseService.getDatabase();
		if (database == null) {
			String message = "No database provider could be found.";
			log.error(message);
			throw new RuntimeException(message);
		}
		try {
			GraphStorageOptions options = Mesh.mesh().getOptions().getStorageOptions();
			database.init(options, Mesh.vertx());
			// TODO should we perhaps check the db also within the bootstrap initalizer?
			//			DatabaseHelper helper = new DatabaseHelper(database);
			//			helper.init();
			//			helper.migrate();
			return database;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void applyChanges() {

		List<Change> list = ChangesList.getList();
		for (Change change : list) {
			// Execute each change in a new transaction
			getDatabase().trx(() -> {
				change.apply();
				//change.validate();
				return change.doesForceReindex();
				// TODO mark change as executed and set the reindex flag if desired
			});
		}
	}
}
