package com.gentics.mesh.changelog;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

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

	private LegacyDatabase db;

	public ChangelogSystem(LegacyDatabase db) {
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
			Tx tx = db.rawTx();
			change.setTx(tx);
			try {
				if (!change.isApplied()) {
					log.info("Handling change {" + change.getUuid() + "}");
					log.info("Name: " + change.getName());
					log.info("Description: " + change.getDescription());

					long start = System.currentTimeMillis();
					change.apply();
					tx.commit();
					change.setDuration(System.currentTimeMillis() - start);
					if (!change.validate()) {
						throw new Exception("Validation for change {" + change.getUuid() + "/" + change.getName() + "} failed.");
					}
					change.markAsComplete();
					reindex |= change.requiresReindex();
				} else {
					log.debug("Change {" + change.getUuid() + "} is already applied.");
				}

			} catch (Exception e) {
				log.error("Error while handling change {" + change.getUuid() + "/" + change.getName() + "}. Invoking rollback..", e);
				tx.rollback();
				return false;
			} finally {
				tx.close();
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
		Tx tx = db.rawTx();
		try {
			for (Change change : list) {
				change.setTx(tx);
				change.markAsComplete();
				log.info("Marking change {" + change.getUuid() + "/" + change.getName() + "} as completed.");
			}
		} finally {
			tx.close();
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
		Tx tx = db.rawTx();
		try {
			Vertex root = MeshGraphHelper.getMeshRootVertex(tx);
			String rev = db.getDatabaseRevision();
			root.property(MESH_VERSION, currentVersion);
			root.property(MESH_DB_REV, rev);
			tx.commit();
		} finally {
			tx.close();
		}
	}
}
