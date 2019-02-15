package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMEA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
		EventQueueBatch batch = EventQueueBatch.create();
		SchemaMigrationMeshEventModel event = new SchemaMigrationMeshEventModel();
		event.setEvent(SCHEMEA_MIGRATION_START);

		SchemaContainerVersion toVersion = getToSchemaVersion();
		event.setToVersion(toVersion.transformToReference());

		SchemaContainerVersion fromVersion = getFromSchemaVersion();
		event.setFromVersion(fromVersion.transformToReference());

		Branch branch = getBranch();
		Project project = branch.getProject();
		event.setProject(project.transformToReference());
		event.setBranch(branch.transformToReference());
		batch.add(event).dispatch();
	}

	protected Completable processTask() {

		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.schema);

		return Completable.defer(() -> {
			AtomicReference<Project> projectRef = new AtomicReference<>(null);
			AtomicReference<Branch> branchRef = new AtomicReference<>(null);
			AtomicReference<SchemaContainerVersion> fromContainerVersionRef = new AtomicReference<>(null);
			AtomicReference<SchemaContainerVersion> toContainerVersionRef = new AtomicReference<>(null);

			try (Tx tx = DB.get().tx()) {

				Branch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}
				branchRef.set(branch);

				SchemaContainerVersion fromContainerVersion = getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
				}
				fromContainerVersionRef.set(fromContainerVersion);

				SchemaContainerVersion toContainerVersion = getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
				}
				toContainerVersionRef.set(toContainerVersion);

				SchemaContainer schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
				}

				Project project = branch.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
				}
				projectRef.set(project);

				BranchSchemaEdge branchVersionEdge = branch.findBranchSchemaEdge(toContainerVersion);
				status.setVersionEdge(branchVersionEdge);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
					+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
					+ "} in project {" + project.getUuid() + "}");

				status.commit();
				tx.success();
			} catch (Exception e) {
				DB.get().tx(() -> {
					status.error(e, "Error while preparing node migration.");
				});
				throw e;
			}

			NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
			// for (int i = 0; i < MIGRATION_ATTEMPT_COUNT; i++) {
			Completable migration = MeshInternal.get().nodeMigrationHandler()
				.migrateNodes(ac, projectRef.get(), branchRef.get(), fromContainerVersionRef.get(), toContainerVersionRef.get(), status);
			migration = migration.doOnComplete(() -> {
				DB.get().tx(() -> {
					JobWarningList warnings = new JobWarningList();
					if (!ac.getConflicts().isEmpty()) {
						for (ConflictWarning conflict : ac.getConflicts()) {
							log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
							warnings.add(conflict);
						}
					}
					setWarnings(warnings);
					finalizeMigration(projectRef.get(), branchRef.get(), fromContainerVersionRef.get());
					status.done();
				});
			}).doOnError(err -> {
				DB.get().tx(() -> {
					status.error(err, "Error in node migration.");
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
			return migration;
		});
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
		// TODO Use events here instead
		// MeshEvent.NODE_MIGRATION_FINISHED
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
