package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

import io.reactivex.Completable;

public interface BranchMigrationJob extends JobCore {

	/**
	 * Create a new branch migration event.
	 * 
	 * @param event
	 * @param status
	 * @return
	 */
	default BranchMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		BranchMigrationMeshEventModel model = new BranchMigrationMeshEventModel();
		model.setEvent(event);

		HibBranch newBranch = getBranch();
		model.setBranch(newBranch.transformToReference());

		HibProject project = newBranch.getProject();
		model.setProject(project.transformToReference());

		model.setStatus(status);
		model.setOrigin(Tx.get().data().options().getNodeName());
		return model;
	}

	private BranchMigrationContext prepareContext(Database db) {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(this, JobType.branch);
		log.debug("Preparing branch migration job");
		try {
			return db.tx(tx -> {
				BranchMigrationContextImpl context = new BranchMigrationContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(BRANCH_MIGRATION_START, STARTING)).dispatch();

				HibBranch newBranch = getBranch();
				if (newBranch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} cannot be found.");
				}
				if (newBranch.isMigrated()) {
					throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} is already migrated");
				}
				context.setNewBranch(newBranch);

				HibBranch oldBranch = newBranch.getPreviousBranch();
				if (oldBranch == null) {
					throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} does not have previous branch");
				}
				if (!oldBranch.isMigrated()) {
					throw error(BAD_REQUEST, "Cannot migrate nodes to branch {" + newBranch.getName() + "}, because previous branch {"
						+ oldBranch.getName() + "} is not fully migrated yet.");
				}
				context.setOldBranch(oldBranch);

				BranchMigrationCause cause = new BranchMigrationCause();
				cause.setProject(newBranch.getProject().transformToReference());
				cause.setOrigin(tx.data().options().getNodeName());
				cause.setUuid(getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db.tx(() -> {
				status.error(e, "Error while preparing branch migration.");
			});
			throw e;
		}
	}

	default Completable processTask(Database db) {
		BranchMigration handler = CommonTx.get().data().mesh().branchMigrationHandler();

		return Completable.defer(() -> {
			BranchMigrationContext context = prepareContext(db);

			return handler.migrateBranch(context)
				.doOnComplete(() -> {
					db.tx(() -> {
						finalizeMigration(db, context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					db.tx(tx -> {
						context.getStatus().error(err, "Error in branch migration.");
						tx.createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});

		});

	}

	private void finalizeMigration(Database db, BranchMigrationContext context) {
		// Mark branch as active & migrated
		db.tx(() -> {
			HibBranch branch = context.getNewBranch();
			branch.setActive(true);
			branch.setMigrated(true);
		});
		db.tx(tx -> {
			tx.createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}
}
