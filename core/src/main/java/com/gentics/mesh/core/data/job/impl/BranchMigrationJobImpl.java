package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.endpoint.migration.branch.BranchMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BranchMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(BranchMigrationJobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void prepare() {
		EventQueueBatch batch = EventQueueBatch.create();
		BranchMigrationMeshEventModel event = new BranchMigrationMeshEventModel();
		event.setEvent(BRANCH_MIGRATION_START);

		Branch newBranch = getBranch();
		event.setBranch(newBranch.transformToReference());

		Project project = newBranch.getProject();
		event.setProject(project.transformToReference());
		batch.add(event).dispatch();
	}

	private BranchMigrationContext prepareContext() {
		return DB.get().tx(() -> {
			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setStatus(new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.branch));

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
			cause.setOrigin(Mesh.mesh().getOptions().getNodeName());
			cause.setUuid(getUuid());
			context.setCause(cause);

			context.getStatus().commit();
			return context;
		});
	}

	@Override
	protected Completable processTask() {
		BranchMigrationHandler handler = MeshInternal.get().branchMigrationHandler();

		return Completable.defer(() -> {
			BranchMigrationContext context = prepareContext();

			try (Tx tx = DB.get().tx()) {
				if (log.isDebugEnabled()) {
					log.debug("Branch migration for job {" + getUuid() + "} was requested");
				}

				try {

					context.getStatus().commit();
					tx.success();
				} catch (Exception e) {
					DB.get().tx(() -> {
						context.getStatus().error(e, "Error while preparing branch migration.");
					});
					throw e;
				}

				Completable migration = handler.migrateBranch(context);
				return migration.doOnComplete(() -> {
					DB.get().tx(() -> {
						finalizeMigration(context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					DB.get().tx(() -> {
						context.getStatus().error(err, "Error in branch migration.");
					});
				});
			}
		});

	}

	private void finalizeMigration(BranchMigrationContext context) {
		// Mark branch as active
		try (Tx tx = DB.get().tx()) {
			Branch branch = context.getNewBranch();
			branch.setActive(true);
			tx.success();
		}
		EventBus eb = Mesh.vertx().eventBus();
		eb.publish(MeshEvent.BRANCH_MIGRATION_FINISHED.address, null);
	}

}
