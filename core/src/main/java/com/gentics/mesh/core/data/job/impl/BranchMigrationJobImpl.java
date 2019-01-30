package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
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
		Branch newBranch = getBranch();
		String newBranchUuid = newBranch.getUuid();
		Project project = newBranch.getProject();

		// Add the needed indices and mappings
		EventQueueBatch indexCreationBatch = new EventQueueBatchImpl();
		for (SchemaContainerVersion schemaVersion : newBranch.findActiveSchemaVersions()) {
			SchemaModel schema = schemaVersion.getSchema();
			// TODO fire - schema migration prepare event
		}
		indexCreationBatch.dispatch();
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
