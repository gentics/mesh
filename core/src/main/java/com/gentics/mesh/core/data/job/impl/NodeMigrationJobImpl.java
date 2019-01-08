package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationJobImpl.class);
	private static final int MIGRATION_ATTEMPT_COUNT = 3;

	public static void init(Database database) {
		database.addVertexType(NodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Create the needed indices.
	 */
	@Override
	public void prepare() {
		Branch branch = getBranch();
		Project project = branch.getProject();
		SchemaContainerVersion toVersion = getToSchemaVersion();
		SchemaModel newSchema = toVersion.getSchema();

		// New indices need to be created
		SearchQueueBatch batch = MeshInternal.get().searchQueue().create();
		batch.createNodeIndex(project.getUuid(), branch.getUuid(), toVersion.getUuid(), DRAFT, newSchema);
		batch.createNodeIndex(project.getUuid(), branch.getUuid(), toVersion.getUuid(), PUBLISHED, newSchema);
		batch.processSync();
	}

	protected Completable processTask() {

		Project project;
		Branch branch;
		SchemaContainerVersion fromContainerVersion;
		SchemaContainerVersion toContainerVersion;
		MigrationStatusHandler status;

		try (Tx tx = DB.get().tx()) {
			status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.schema);
			branch = getBranch();
			if (branch == null) {
				throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
			}

			fromContainerVersion = getFromSchemaVersion();
			if (fromContainerVersion == null) {
				throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
			}

			toContainerVersion = getToSchemaVersion();
			if (toContainerVersion == null) {
				throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
			}

			SchemaContainer schemaContainer = toContainerVersion.getSchemaContainer();
			if (schemaContainer == null) {
				throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
			}

			project = branch.getProject();
			if (project == null) {
				throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
			}

			BranchSchemaEdge branchVersionEdge = branch.findBranchSchemaEdge(toContainerVersion);
			status.setVersionEdge(branchVersionEdge);

			log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
				+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
				+ "} in project {" + project.getUuid() + "}");

			status.commit();
			tx.success();

		}
		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
		// for (int i = 0; i < MIGRATION_ATTEMPT_COUNT; i++) {
		Completable migration = MeshInternal.get().nodeMigrationHandler()
			.migrateNodes(ac, project, branch, fromContainerVersion, toContainerVersion, status).doOnComplete(() -> {
				DB.get().tx(() -> {
					JobWarningList warnings = new JobWarningList();
					if (!ac.getConflicts().isEmpty()) {
						for (ConflictWarning conflict : ac.getConflicts()) {
							log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
							warnings.add(conflict);
						}
					}
					setWarnings(warnings);
					finalizeMigration(project, branch, fromContainerVersion);
					status.done();
				});
			});
		// // Check migration result
		// boolean hasRemainingContainers = fromContainerVersion.getDraftFieldContainers(branch.getUuid()).hasNext();
		// if (i == MIGRATION_ATTEMPT_COUNT - 1 && hasRemainingContainers) {
		// log.error("There were still not yet migrated containers after {" + i + "} migration runs.");
		// } else if (hasRemainingContainers) {
		// log.info("Found not yet migrated containers for schema version {" + fromContainerVersion.getName() + "@"
		// + fromContainerVersion.getVersion() + "} invoking migration again.");
		// } else {
		// break;
		// }
		// }
		migration = migration.doOnError(err -> {
			DB.get().tx(() -> {
				status.error(err, "Error while preparing node migration.");
			});
		});
		return migration;
	}

	private void finalizeMigration(Project project, Branch branch, SchemaContainerVersion fromContainerVersion) {
		// Deactivate edge
		try (Tx tx = DB.get().tx()) {
			BranchSchemaEdge edge = branch.findBranchSchemaEdge(fromContainerVersion);
			if (edge != null) {
				edge.setActive(false);
			}
			tx.success();
		}
		// Remove old indices
		MeshInternal.get().searchProvider()
			.deleteIndex(NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), fromContainerVersion.getUuid(), DRAFT))
			.blockingAwait();
		MeshInternal.get().searchProvider()
			.deleteIndex(
				NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), fromContainerVersion.getUuid(), PUBLISHED))
			.blockingAwait();

	}

}
