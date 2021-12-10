package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(NodeJobProcessor.class);
	private Database db;

	@Inject
	public NodeJobProcessor(Database db) {
		this.db = db;
	}

	private SchemaMigrationMeshEventModel createEvent(HibJob job, Tx tx, MeshEvent event, JobStatus status) {
		SchemaMigrationMeshEventModel model = new SchemaMigrationMeshEventModel();
		model.setEvent(event);

		HibSchemaVersion toVersion = job.getToSchemaVersion();
		model.setToVersion(toVersion.transformToReference());

		HibSchemaVersion fromVersion = job.getFromSchemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		HibBranch branch = job.getBranch();
		HibProject project = branch.getProject();
		model.setProject(project.transformToReference());
		model.setBranch(branch.transformToReference());

		model.setOrigin(tx.data().options().getNodeName());
		model.setStatus(status);
		return model;
	}

	private NodeMigrationActionContextImpl prepareContext(HibJob job) {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(job.getUuid());
		try {
			return db.tx(tx -> {
				NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(job, tx, SCHEMA_MIGRATION_START, STARTING)).dispatch();

				HibBranch branch = job.getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + job.getUuid() + "} not found");
				}
				context.setBranch(branch);

				HibSchemaVersion fromContainerVersion = job.getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + job.getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				HibSchemaVersion toContainerVersion = job.getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + job.getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				HibSchema schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + job.getUuid() + "} can't be found.");
				}

				HibProject project = branch.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + job.getUuid() + "} not found");
				}
				context.setProject(project);

				HibBranchSchemaVersion branchVersionAssignment = Tx.get().branchDao().findBranchSchemaEdge(branch, toContainerVersion);
				context.getStatus().setVersionEdge(branchVersionAssignment);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
						+ "} in project {" + project.getUuid() + "}");

				SchemaMigrationCause cause = new SchemaMigrationCause();
				cause.setFromVersion(fromContainerVersion.transformToReference());
				cause.setToVersion(toContainerVersion.transformToReference());
				cause.setProject(project.transformToReference());
				cause.setBranch(branch.transformToReference());
				cause.setOrigin(tx.data().options().getNodeName());
				cause.setUuid(job.getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db.tx(() -> {
				status.error(e, "Error while preparing node migration.");
			});
			throw e;
		}
	}

	@Override
	public Completable process(HibJob job) {
		NodeMigration handler = db.tx(tx -> {
			return tx.<CommonTx>unwrap().data().mesh().nodeMigrationHandler();
		});
		return Completable.defer(() -> {
			NodeMigrationActionContextImpl context = prepareContext(job);

			return handler.migrateNodes(context)
					.doOnComplete(() -> {
						db.tx(() -> {
							JobWarningList warnings = new JobWarningList();
							if (!context.getConflicts().isEmpty()) {
								for (ConflictWarning conflict : context.getConflicts()) {
									log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved.");
									warnings.add(conflict);
								}
							}
							job.setWarnings(warnings);
							finalizeMigration(job, context);
							context.getStatus().done();
						});
					}).doOnError(err -> {
						db.tx(tx -> {
							context.getStatus().error(err, "Error in node migration.");
							tx.createBatch().add(createEvent(job, tx, SCHEMA_MIGRATION_FINISHED, FAILED)).dispatch();
						});
					});

		});
	}

	private void finalizeMigration(HibJob job, NodeMigrationActionContext context) {
		// Deactivate edge
		db.tx(() -> {
			HibBranch branch = context.getBranch();
			HibSchemaVersion fromContainerVersion = context.getFromVersion();
			HibBranchSchemaVersion assignment =  Tx.get().branchDao().findBranchSchemaEdge(branch, fromContainerVersion);
			if (assignment != null) {
				assignment.setActive(false);
			}
			if (log.isDebugEnabled()) {
				log.debug("Deactivated schema version {}-{} for branch {}",
						fromContainerVersion.getSchema().getName(), fromContainerVersion.getVersion(), branch.getName());
			}
		});
		db.tx(tx -> {
			tx.createBatch().add(createEvent(job, tx, SCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}
}
