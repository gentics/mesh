package com.gentics.mesh.changelog;

import java.util.List;

import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.TransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(ChangelogSystem.class);

	private Database db;

	public ChangelogSystem(Database db) {
		this.db = db;
	}

	/**
	 * Apply all listed changes.
	 * 
	 * @return Flag which indicates whether all changes were applied successfully
	 */
	public boolean applyChanges() {

		List<Change> list = ChangesList.getList();
		for (Change change : list) {
			// Execute each change in a new transaction
			TransactionalGraph graph = db.rawTx();
			change.setGraph(graph);
			try {
				if (!change.isApplied()) {
					log.info("Handling change {" + change.getUuid() + "}");
					log.info("Name: " + change.getName());
					log.info("Description: " + change.getDescription());

					long start = System.currentTimeMillis();
					change.apply();
					change.setDuration(System.currentTimeMillis() - start);
					change.markAsComplete();
					//change.validate();
					change.doesForceReindex();
					// TODO mark change as executed and set the reindex flag if desired
				} else {
					log.debug("Change {" + change.getUuid() + "} is already applied.");
				}
			} catch (Exception e) {
				log.error("Error while handling change {" + change.getUuid() + "/" + change.getName() + "}", e);
				graph.rollback();
				return false;
			} finally {
				graph.shutdown();
			}
		}
		return true;
	}
}
