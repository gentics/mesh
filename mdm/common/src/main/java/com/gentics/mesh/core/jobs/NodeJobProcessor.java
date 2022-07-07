package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import javax.inject.Inject;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class is responsible for starting a node migration from a job
 */
public class NodeJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(NodeJobProcessor.class);
	private Database db;

	@Inject
	public NodeJobProcessor(Database db) {
		this.db = db;
	}

	@Override
	public Completable process(HibJob job) {
		NodeMigration handler = db.tx(tx -> {
			return tx.<CommonTx>unwrap().data().mesh().nodeMigrationHandler();
		});
		return Completable.defer(() -> {
			NodeMigrationActionContextImpl context = handler.prepareContext(job);

			return handler.migrateNodes(context)
					.doOnComplete(() -> {
						db.tx(() -> {
							JobWarningList warnings = new JobWarningList();
							if (!context.getConflicts().isEmpty()) {
								for (ConflictWarning conflict : context.getConflicts()) {
									log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
									warnings.add(conflict);
								}
							}
							finalizeMigration(handler, job, context);
							context.getStatus().done(warnings);
						});
					}).doOnError(err -> {
						db.tx(tx -> {
							context.getStatus().error(err, "Error in node migration.");
							tx.createBatch().add(handler.createEvent(job, tx, SCHEMA_MIGRATION_FINISHED, FAILED)).dispatch();
						});
					});

		});
	}

	private void finalizeMigration(NodeMigration handler, HibJob job, NodeMigrationActionContext context) {
		// Deactivate edge
		db.tx(() -> {
			HibBranch branch = context.getBranch();
			HibSchemaVersion fromContainerVersion = context.getFromVersion();
			HibBranchSchemaVersion assignment =  Tx.get().branchDao().findBranchSchemaEdge(branch, fromContainerVersion);
			if (assignment != null) {
				assignment.setActive(false);
			}
			if (log.isDebugEnabled()) {
				log.debug("Deactivated schema version {}-{} for branch {}",
						fromContainerVersion.getSchema().getName(), fromContainerVersion.getVersion(), branch.getName());
			}
		});
		db.tx(tx -> {
			tx.createBatch().add(handler.createEvent(job, tx, SCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}
}
