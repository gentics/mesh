package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationJobImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	private SchemaMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		SchemaMigrationMeshEventModel model = new SchemaMigrationMeshEventModel();
		model.setEvent(event);

		HibSchemaVersion toVersion = getToSchemaVersion();
		model.setToVersion(toVersion.transformToReference());

		HibSchemaVersion fromVersion = getFromSchemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		HibBranch branch = getBranch();
		HibProject project = branch.getProject();
		model.setProject(project.transformToReference());
		model.setBranch(branch.transformToReference());

		model.setOrigin(options().getNodeName());
		model.setStatus(status);
		return model;
	}

	private NodeMigrationActionContextImpl prepareContext() {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(this, JobType.schema);
		try {
			return db().tx(() -> {
				NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
				context.setStatus(status);

				createBatch().add(createEvent(SCHEMA_MIGRATION_START, STARTING)).dispatch();

				HibBranch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}
				context.setBranch(branch);

				HibSchemaVersion fromContainerVersion = getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				HibSchemaVersion toContainerVersion = getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				HibSchema schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
				}

				HibProject project = branch.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
				}
				context.setProject(project);

				HibBranchSchemaVersion branchVersionAssignment = branch.findBranchSchemaEdge(toContainerVersion);
				context.getStatus().setVersionEdge(branchVersionAssignment);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
					+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
					+ "} in project {" + project.getUuid() + "}");

				SchemaMigrationCause cause = new SchemaMigrationCause();
				cause.setFromVersion(fromContainerVersion.transformToReference());
				cause.setToVersion(toContainerVersion.transformToReference());
				cause.setProject(project.transformToReference());
				cause.setBranch(branch.transformToReference());
				cause.setOrigin(options().getNodeName());
				cause.setUuid(getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db().tx(() -> {
				status.error(e, "Error while preparing node migration.");
			});
			throw e;
		}
	}

	protected Completable processTask() {
		NodeMigration handler = mesh().nodeMigrationHandler();

		return Completable.defer(() -> {
			NodeMigrationActionContextImpl context = prepareContext();

			return handler.migrateNodes(context)
				.doOnComplete(() -> {
					db().tx(() -> {
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
					db().tx(() -> {
						context.getStatus().error(err, "Error in node migration.");
						createBatch().add(createEvent(SCHEMA_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});

		});
	}

	private void finalizeMigration(NodeMigrationActionContext context) {
		// Deactivate edge
		db().tx(() -> {
			HibBranch branch = context.getBranch();
			HibSchemaVersion fromContainerVersion = context.getFromVersion();
			HibBranchSchemaVersion assignment = branch.findBranchSchemaEdge(fromContainerVersion);
			if (assignment != null) {
				assignment.setActive(false);
			}
			if (log.isDebugEnabled()) {
				log.debug("Deactivated schema version {}-{} for branch {}",
					fromContainerVersion.getSchema().getName(), fromContainerVersion.getVersion(), branch.getName());
			}
		});
		db().tx(() -> {
			createBatch().add(createEvent(SCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
