package com.gentics.mesh.changelog;

import java.util.List;

import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.TransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central changelog system class which is responsible for handling listed changes.
 * 
 * A change must be added to the {@link ChangesList} in order to be handled by the {@link ChangelogSystem}.
 */
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
					if (!change.validate()) {
						throw new Exception("Validation for change {" + change.getUuid() + "/" + change.getName() + "} failed.");
					}
					change.markAsComplete();
				} else {
					log.debug("Change {" + change.getUuid() + "} is already applied.");
				}
			} catch (Exception e) {
				log.error("Error while handling change {" + change.getUuid() + "/" + change.getName() + "}. Invoking rollback..", e);
				graph.rollback();
				return false;
			} finally {
				graph.shutdown();
			}
		}
		return true;
	}

	/**
	 * Mark all changelog entries as applied. This is useful if you resolved issues manually or if you want to create a fresh mesh database dump.
	 */
	public void markAllAsApplied() {
		List<Change> list = ChangesList.getList();
		TransactionalGraph graph = db.rawTx();
		try {
			for (Change change : list) {
				change.setGraph(graph);
				change.markAsComplete();
				log.info("Marking change {" + change.getUuid() + "/" + change.getName() + "} as complete.");
			}
		} finally {
			graph.shutdown();
		}
	}
}
