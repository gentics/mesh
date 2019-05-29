package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
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
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(NodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	private SchemaMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		SchemaMigrationMeshEventModel model = new SchemaMigrationMeshEventModel();
		model.setEvent(event);

		SchemaContainerVersion toVersion = getToSchemaVersion();
		model.setToVersion(toVersion.transformToReference());

		SchemaContainerVersion fromVersion = getFromSchemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		Branch branch = getBranch();
		Project project = branch.getProject();
		model.setProject(project.transformToReference());
		model.setBranch(branch.transformToReference());

		model.setOrigin(Mesh.mesh().getOptions().getNodeName());
		model.setStatus(status);
		return model;
	}

	private NodeMigrationActionContextImpl prepareContext() {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), JobType.schema);
		try {
			return DB.get().tx(() -> {
				NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
				context.setStatus(status);

				EventQueueBatch.create().add(createEvent(SCHEMA_MIGRATION_START, STARTING)).dispatch();

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
		} catch (Exception e) {
			DB.get().tx(() -> {
				status.error(e, "Error while preparing node migration.");
			});
			throw e;
		}
	}

	protected Completable processTask() {
		NodeMigrationHandler handler = MeshInternal.get().nodeMigrationHandler();

		return Completable.defer(() -> {
			NodeMigrationActionContextImpl context = prepareContext();

			return handler.migrateNodes(context)
				.doOnComplete(() -> {
					DB.get().tx(() -> {
						JobWarningList warnings = new JobWarningList();
						if (!context.getConflicts().isEmpty()) {
							for (ConflictWarning conflict : context.getConflicts()) {
								log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
								warnings.add(conflict);
							}
						}
						setWarnings(warnings);
						finalizeMigration(context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					DB.get().tx(() -> {
						context.getStatus().error(err, "Error in node migration.");
						EventQueueBatch.create().add(createEvent(SCHEMA_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});

		});
	}

	private void finalizeMigration(NodeMigrationActionContext context) {
		// Deactivate edge
		DB.get().tx(() -> {
			Branch branch = context.getBranch();
			SchemaContainerVersion fromContainerVersion = context.getFromVersion();
			BranchSchemaEdge edge = branch.findBranchSchemaEdge(fromContainerVersion);
			if (edge != null) {
				edge.setActive(false);
			}
		});
		DB.get().tx(() -> {
			EventQueueBatch.create().add(createEvent(SCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
