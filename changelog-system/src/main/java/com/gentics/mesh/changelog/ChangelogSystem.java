package com.gentics.mesh.changelog;

import java.util.List;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central changelog system class which is responsible for handling listed changes.
 * 
 * A change must be added to the {@link ChangesList} in order to be handled by the {@link ChangelogSystem}.
 */
public class ChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(ChangelogSystem.class);

	public static final String MESH_VERSION = "meshVersion";

	public static final String MESH_DB_REV = "meshDatabaseRevision";

	private Database db;

	public ChangelogSystem(Database db) {
		this.db = db;
	}

	/**
	 * Apply all listed changes.
	 * 
	 * @param reindexAction
	 * @param list
	 * @return Flag which indicates whether all changes were applied successfully
	 */
	public boolean applyChanges(ReindexAction reindexAction, List<Change> list) {
		boolean reindex = false;
		for (Change change : list) {
			// Execute each change in a new transaction
			change.setDb(db);
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
					reindex |= change.requiresReindex();
				} else {
					log.debug("Change {" + change.getUuid() + "} is already applied.");
				}
			} catch (Throwable e) {
				log.error("Error while handling change {" + change.getUuid() + "/" + change.getName() + "}.", e);
				return false;
			}
		}
		if (reindex) {
			reindexAction.invoke();
		}
		return true;
	}

	/**
	 * Mark all changelog entries as applied. This is useful if you resolved issues manually or if you want to create a fresh mesh database dump.
	 */
	public void markAllAsApplied(List<Change> list) {
		TransactionalGraph graph = db.rawTx();
		try {
			for (Change change : list) {
				change.setGraph(graph);
				change.setDb(db);
				change.markAsComplete();
				log.info("Marking change {" + change.getUuid() + "/" + change.getName() + "} as completed.");
			}
		} finally {
			graph.shutdown();
		}
	}

	/**
	 * Apply all changes from the {@link ChangesList}.
	 * 
	 * @param reindexAction
	 * @return
	 */
	public boolean applyChanges(ReindexAction reindexAction) {
		return applyChanges(reindexAction, ChangesList.getList());
	}

	/**
	 * Mark all changes from the {@link ChangesList} as applied.
	 */
	public void markAllAsApplied() {
		markAllAsApplied(ChangesList.getList());
		setCurrentVersionAndRev();
	}

	/**
	 * Update the internally stored database version and mesh version in the mesh root vertex.
	 */
	public void setCurrentVersionAndRev() {
		log.info("Updating stored database revision and mesh version.");
		// Version is okay. So lets store the version and the updated revision.
		String currentVersion = Mesh.getPlainVersion();
		TransactionalGraph graph = db.rawTx();
		try {
			Vertex root = MeshGraphHelper.getMeshRootVertex(graph);
			String rev = db.getDatabaseRevision();
			root.setProperty(MESH_VERSION, currentVersion);
			root.setProperty(MESH_DB_REV, rev);
			graph.commit();
		} finally {
			graph.shutdown();
		}
	}
}
