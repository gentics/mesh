package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.MicronodeMigrationContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of the micronode migration job.
 */
public class MicronodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationJobImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Create a new migration event model object.
	 * 
	 * @param event
	 * @param status
	 * @return
	 */
	public MicroschemaMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		MicroschemaMigrationMeshEventModel model = new MicroschemaMigrationMeshEventModel();
		model.setEvent(event);

		HibMicroschemaVersion toVersion = getToMicroschemaVersion();
		model.setToVersion(toVersion.transformToReference());

		HibMicroschemaVersion fromVersion = getFromMicroschemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		HibBranch branch = getBranch();
		if (branch != null) {
			HibProject project = branch.getProject();
			model.setProject(project.transformToReference());
			model.setBranch(branch.transformToReference());
		}

		model.setOrigin(options().getNodeName());
		model.setStatus(status);
		return model;
	}

	private MicronodeMigrationContext prepareContext() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, JobType.microschema);
		try {
			return db().tx(tx -> {
				MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(MICROSCHEMA_MIGRATION_START, STARTING)).dispatch();

				HibBranch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}
				context.setBranch(branch);

				HibMicroschemaVersion fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				HibMicroschemaVersion toContainerVersion = getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				HibMicroschema schemaContainer = fromContainerVersion.getSchemaContainer();
				HibBranchMicroschemaVersion branchVersionEdge = branch.findBranchMicroschemaEdge(toContainerVersion);
				context.getStatus().setVersionEdge(branchVersionEdge);
				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}

				MicroschemaMigrationCause cause = new MicroschemaMigrationCause();
				cause.setFromVersion(fromContainerVersion.transformToReference());
				cause.setToVersion(toContainerVersion.transformToReference());
				cause.setBranch(branch.transformToReference());
				cause.setOrigin(options().getNodeName());
				cause.setUuid(getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db().tx(() -> {
				status.error(e, "Error while preparing micronode migration.");
			});
			throw e;
		}

	}

	@Override
	public Completable processTask(Database db) {
		return Completable.defer(() -> {
			MicronodeMigrationContext context = prepareContext();
			MicronodeMigration handler = mesh().micronodeMigrationHandler();
			return handler.migrateMicronodes(context)
				.doOnComplete(() -> {
					db.tx(() -> {
						JobWarningList warnings = new JobWarningList();
						setWarnings(warnings);
						finializeMigration(db, context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					db.tx(tx -> {
						context.getStatus().error(err, "Error in micronode migration.");
						tx.createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});
		});
	}

	private void finializeMigration(Database db, MicronodeMigrationContext context) {
		db.tx(tx -> {
			tx.createBatch().add(createEvent(MICROSCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
