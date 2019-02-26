package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.vertx.core.eventbus.EventBus;
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
		event.setEvent(SCHEMA_MIGRATION_START);

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

	private NodeMigrationActionContextImpl prepareContext() {
		return DB.get().tx(() -> {
			NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();

			context.setStatus(new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.schema));

			Branch branch = getBranch();
			if (branch == null) {
				throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
			}
			context.setBranch(branch);

			SchemaContainerVersion fromContainerVersion = getFromSchemaVersion();
			if (fromContainerVersion == null) {
				throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
			}
			context.setFromVersion(fromContainerVersion);

			SchemaContainerVersion toContainerVersion = getToSchemaVersion();
			if (toContainerVersion == null) {
				throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
			}
			context.setToVersion(toContainerVersion);

			SchemaContainer schemaContainer = toContainerVersion.getSchemaContainer();
			if (schemaContainer == null) {
				throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
			}

			Project project = branch.getProject();
			if (project == null) {
				throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
			}
			context.setProject(project);

			BranchSchemaEdge branchVersionEdge = branch.findBranchSchemaEdge(toContainerVersion);
			context.getStatus().setVersionEdge(branchVersionEdge);

			log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
				+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
				+ "} in project {" + project.getUuid() + "}");

			SchemaMigrationCause cause = new SchemaMigrationCause();
			cause.setFromVersion(fromContainerVersion.transformToReference());
			cause.setToVersion(toContainerVersion.transformToReference());
			cause.setProject(project.transformToReference());
			cause.setBranch(branch.transformToReference());
			cause.setOrigin(Mesh.mesh().getOptions().getNodeName());
			cause.setUuid(getUuid());
			context.setCause(cause);

			context.getStatus().commit();
			return context;
		});

		// catch (Exception e) {
		// DB.get().tx(() -> {
		// ac.getStatus().error(e, "Error while preparing node migration.");
		// });
		// throw e;
		// }
	}

	protected Completable processTask() {
		NodeMigrationHandler handler = MeshInternal.get().nodeMigrationHandler();

		return Completable.defer(() -> {
			NodeMigrationActionContextImpl ac = prepareContext();

			Completable migration = handler.migrateNodes(ac);
			return migration.doOnComplete(() -> {
				DB.get().tx(() -> {
					JobWarningList warnings = new JobWarningList();
					if (!ac.getConflicts().isEmpty()) {
						for (ConflictWarning conflict : ac.getConflicts()) {
							log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
							warnings.add(conflict);
						}
					}
					setWarnings(warnings);
					finalizeMigration(ac);
					ac.getStatus().done();
				});
			}).doOnError(err -> {
				DB.get().tx(() -> {
					ac.getStatus().error(err, "Error in node migration.");
				});
			});

		});
	}

	private void finalizeMigration(NodeMigrationActionContext context) {
		// Deactivate edge
		try (Tx tx = DB.get().tx()) {
			Branch branch = context.getBranch();
			SchemaContainerVersion fromContainerVersion = context.getFromVersion();
			BranchSchemaEdge edge = branch.findBranchSchemaEdge(fromContainerVersion);
			if (edge != null) {
				edge.setActive(false);
			}
			tx.success();
		}
		EventBus eb = Mesh.vertx().eventBus();
		eb.publish(MeshEvent.SCHEMA_MIGRATION_FINISHED.address, null);
	}

}
