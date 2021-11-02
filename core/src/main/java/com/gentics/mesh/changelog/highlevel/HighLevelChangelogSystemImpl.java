package com.gentics.mesh.changelog.highlevel;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.cli.PostProcessFlags;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Manager class for the high level changelog system.
 */
@Singleton
public class HighLevelChangelogSystemImpl implements HighLevelChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(HighLevelChangelogSystemImpl.class);

	private final Database db;

	private final HighLevelChangesList highLevelChangesList;

	@Inject
	public HighLevelChangelogSystemImpl(Database db, HighLevelChangesList highLevelChangesList) {
		this.db = db;
		this.highLevelChangesList = highLevelChangesList;
	}

	@Override
	public void apply(PostProcessFlags flags, MeshRoot meshRoot, Predicate<? super HighLevelChange> filter) {
		List<HighLevelChange> changes = highLevelChangesList.getList();
		for (HighLevelChange change : changes) {
			db.tx(tx2 -> {
				if (!isApplied(meshRoot, change)) {
					// if a filter is given and a change does not pass its test, we fail
					if (filter != null && !filter.test(change)) {
						throw new RuntimeException("Cannot execute change " + change.getName()
								+ " in cluster mode. Please restart a single instance in the cluster with the "
								+ MeshOptions.MESH_CLUSTER_INIT_ENV + " environment flag or the -"
								+ MeshCLI.INIT_CLUSTER + " command line argument to migrate the database.");
					}
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
						if (change.requiresReindex()) {
							flags.requireReindex();
						}
					} catch (Exception e) {
						log.error("Error while executing change {" + change.getName() + "}/{" + change.getUuid() + "}", e);
						throw new RuntimeException("Error while executing high level changelog.");
					}
				}
			});
		}
	}

	@Override
	public void markAllAsApplied(MeshRoot meshRoot) {
		db.tx(tx -> {
			List<HighLevelChange> changes = highLevelChangesList.getList();
			for (HighLevelChange change : changes) {
				meshRoot.getChangelogRoot().add(change, 0);
			}
			tx.success();
		});
	}

	@Override
	public boolean requiresChanges(MeshRoot meshRoot, Predicate<? super HighLevelChange> filter) {
		return db.tx(tx -> {
			Stream<HighLevelChange> stream = highLevelChangesList.getList().stream();
			if (filter != null) {
				stream = stream.filter(filter);
			}
			return stream.filter(change -> !isApplied(meshRoot, change)).findFirst().isPresent();
		});
	}

}
