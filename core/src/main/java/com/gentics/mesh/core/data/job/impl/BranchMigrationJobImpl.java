package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
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

	@Override
	protected Completable processTask() {
		return Completable.fromAction(() -> {
			try (Tx tx = DB.get().tx()) {
				MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.branch);
				try {
					if (log.isDebugEnabled()) {
						log.debug("Branch migration for job {" + getUuid() + "} was requested");
					}
					status.commit();

					Branch branch = getBranch();
					if (branch == null) {
						throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} cannot be found.");
					}
					MeshInternal.get().branchMigrationHandler().migrateBranch(branch, status).blockingAwait();
					status.done();
				} catch (Exception e) {
					status.error(e, "Error while preparing branch migration.");
					throw e;
				}
			}
		});
	}

}
