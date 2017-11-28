package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ReleaseMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(ReleaseMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(ReleaseMigrationJobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void prepare() {
		Release newRelease = getRelease();
		String newReleaseUuid = newRelease.getUuid();
		Project project = newRelease.getProject();

		// Add the needed indices and mappings
		SearchQueueBatch indexCreationBatch = MeshInternal.get().searchQueue().create();
		for (SchemaContainerVersion schemaVersion : newRelease.findActiveSchemaVersions()) {
			SchemaModel schema = schemaVersion.getSchema();
			indexCreationBatch.createNodeIndex(project.getUuid(), newReleaseUuid, schemaVersion.getUuid(), PUBLISHED, schema);
			indexCreationBatch.createNodeIndex(project.getUuid(), newReleaseUuid, schemaVersion.getUuid(), DRAFT, schema);
		}
		indexCreationBatch.processSync();
	}

	@Override
	protected void processTask() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.release);
		try {

			if (log.isDebugEnabled()) {
				log.debug("Release migration for job {" + getUuid() + "} was requested");
			}
			status.commit();

			try (Tx tx = DB.get().tx()) {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} cannot be found.");
				}
				MeshInternal.get().releaseMigrationHandler().migrateRelease(release, status);
				status.done();
			}
		} catch (Exception e) {
			status.error(e, "Error while preparing release migration.");
			throw e;
		}
	}

}
