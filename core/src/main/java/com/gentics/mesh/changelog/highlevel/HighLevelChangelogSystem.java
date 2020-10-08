package com.gentics.mesh.changelog.highlevel;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Manager class for the high level changelog system.
 */
@Singleton
public class HighLevelChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(HighLevelChangelogSystem.class);

	private final Database db;

	private final HighLevelChangesList highLevelChangesList;

	@Inject
	public HighLevelChangelogSystem(Database db, HighLevelChangesList highLevelChangesList) {
		this.db = db;
		this.highLevelChangesList = highLevelChangesList;
	}

	/**
	 * Apply the changes which were not yet applied.
	 * 
	 * @param meshRoot
	 */
	public void apply(MeshRoot meshRoot) {
		List<HighLevelChange> changes = highLevelChangesList.getList();
		for (HighLevelChange change : changes) {
			db.tx(tx2 -> {
				if (!isApplied(meshRoot, change)) {
					try {
						long start = System.currentTimeMillis();
						db.tx(tx -> {
							if (log.isDebugEnabled()) {
								log.debug("Executing change {" + change.getName() + "}/{" + change.getUuid() + "}");
							}
							change.apply();
							tx.success();
						});
						change.applyNoTx();
						long duration = System.currentTimeMillis() - start;
						db.tx(tx -> {
							meshRoot.getChangelogRoot().add(change, duration);
							tx.success();
						});
					} catch (Exception e) {
						log.error("Error while executing change {" + change.getName() + "}/{" + change.getUuid() + "}", e);
						throw new RuntimeException("Error while executing high level changelog.");
					}
				}
			});
		}
	}

	/**
	 * Check whether the change has already been applied.
	 * 
	 * @param root
	 * @param change
	 * @return
	 */
	private boolean isApplied(MeshRoot root, HighLevelChange change) {
		return root.getChangelogRoot().hasChange(change);
	}

	/**
	 * Mark all high level changes as applied.
	 * 
	 * @param meshRoot
	 */
	public void markAllAsApplied(MeshRoot meshRoot) {
		db.tx(tx -> {
			List<HighLevelChange> changes = highLevelChangesList.getList();
			for (HighLevelChange change : changes) {
				meshRoot.getChangelogRoot().add(change, 0);
			}
			tx.success();
		});
	}

	/**
	 * Check whether any high level changelog entry needs to be applied.
	 * 
	 * @param meshRoot
	 * @return
	 */
	public boolean requiresChanges(MeshRoot meshRoot) {
		return db.tx(tx -> {
			List<HighLevelChange> changes = highLevelChangesList.getList();
			for (HighLevelChange change : changes) {
				if (!isApplied(meshRoot, change)) {
					return true;
				}
			}
			return false;
		});
	}

}
