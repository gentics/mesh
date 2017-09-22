package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.release.ReleaseSchemaEdge;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@GraphElement
public class NodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(NodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Create the needed indices.
	 */
	@Override
	public void prepare() {
		Release release = getRelease();
		Project project = release.getProject();
		SchemaContainerVersion toVersion = getToSchemaVersion();
		SchemaModel newSchema = toVersion.getSchema();

		// New indices need to be created
		SearchQueueBatch batch = MeshInternal.get().searchQueue().create();
		batch.createNodeIndex(project.getUuid(), release.getUuid(), toVersion.getUuid(), DRAFT, newSchema);
		batch.createNodeIndex(project.getUuid(), release.getUuid(), toVersion.getUuid(), PUBLISHED, newSchema);
		batch.processSync();
	}

	protected void processTask() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.schema);
		try {

			try (Tx tx = db.tx()) {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} not found");
				}

				SchemaContainerVersion fromContainerVersion = getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
				}

				SchemaContainerVersion toContainerVersion = getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
				}

				SchemaContainer schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
				}

				Project project = release.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
				}

				ReleaseSchemaEdge releaseVersionEdge = release.findReleaseSchemaEdge(toContainerVersion);
				statusHandler.setVersionEdge(releaseVersionEdge);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + release.getUuid()
						+ "} in project {" + project.getUuid() + "}");

				statusHandler.commitStatus();

				MeshInternal.get().nodeMigrationHandler().migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusHandler)
						.await();
				statusHandler.done();
				tx.success();
			}
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing node migration.");
		}
	}

}
