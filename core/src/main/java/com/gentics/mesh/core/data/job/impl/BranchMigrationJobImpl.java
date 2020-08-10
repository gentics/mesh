package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.migration.impl.BranchMigrationImpl;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BranchMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationJobImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BranchMigrationJobImpl.class, MeshVertexImpl.class);
	}

	public BranchMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		BranchMigrationMeshEventModel model = new BranchMigrationMeshEventModel();
		model.setEvent(event);

		Branch newBranch = getBranch();
		model.setBranch(newBranch.transformToReference());

		Project project = newBranch.getProject();
		model.setProject(project.transformToReference());

		model.setStatus(status);
		model.setOrigin(options().getNodeName());
		return model;
	}

	private BranchMigrationContext prepareContext() {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(this, vertx(), JobType.branch);
		try {
			return db().tx(() -> {
				BranchMigrationContextImpl context = new BranchMigrationContextImpl();
				context.setStatus(status);

				createBatch().add(createEvent(BRANCH_MIGRATION_START, STARTING)).dispatch();

				Branch newBranch = getBranch();
				if (newBranch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} cannot be found.");
				}
				if (newBranch.isMigrated()) {
					throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} is already migrated");
				}
				context.setNewBranch(newBranch);

				Branch oldBranch = newBranch.getPreviousBranch();
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
				cause.setOrigin(options().getNodeName());
				cause.setUuid(getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db().tx(() -> {
				status.error(e, "Error while preparing branch migration.");
			});
			throw e;
		}
	}

	@Override
	protected Completable processTask() {
		BranchMigrationImpl handler = mesh().branchMigrationHandler();

		return Completable.defer(() -> {
			BranchMigrationContext context = prepareContext();

			return handler.migrateBranch(context)
				.doOnComplete(() -> {
					db().tx(() -> {
						finalizeMigration(context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					db().tx(() -> {
						context.getStatus().error(err, "Error in branch migration.");
						createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});

		});

	}

	private void finalizeMigration(BranchMigrationContext context) {
		// Mark branch as active & migrated
		db().tx(() -> {
			Branch branch = context.getNewBranch();
			branch.setActive(true);
			branch.setMigrated(true);
		});
		db().tx(() -> {
			createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
