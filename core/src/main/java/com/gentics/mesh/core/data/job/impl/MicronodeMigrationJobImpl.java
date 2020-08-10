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
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.migration.impl.MicronodeMigrationImpl;
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

public class MicronodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationJobImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	public MicroschemaMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		MicroschemaMigrationMeshEventModel model = new MicroschemaMigrationMeshEventModel();
		model.setEvent(event);

		MicroschemaVersion toVersion = getToMicroschemaVersion();
		model.setToVersion(toVersion.transformToReference());

		MicroschemaVersion fromVersion = getFromMicroschemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		Branch branch = getBranch();
		if (branch != null) {
			Project project = branch.getProject();
			model.setProject(project.transformToReference());
			model.setBranch(branch.transformToReference());
		}

		model.setOrigin(options().getNodeName());
		model.setStatus(status);
		return model;
	}

	private MicronodeMigrationContext prepareContext() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, vertx(), JobType.microschema);
		try {
			return db().tx(() -> {
				MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
				context.setStatus(status);

				createBatch().add(createEvent(MICROSCHEMA_MIGRATION_START, STARTING)).dispatch();

				Branch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}
				context.setBranch(branch);

				MicroschemaVersion fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				MicroschemaVersion toContainerVersion = getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				Microschema schemaContainer = fromContainerVersion.getSchemaContainer();
				BranchMicroschemaEdge branchVersionEdge = branch.findBranchMicroschemaEdge(toContainerVersion);
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

	protected Completable processTask() {
		return Completable.defer(() -> {
			MicronodeMigrationContext context = prepareContext();
			MicronodeMigrationImpl handler = mesh().micronodeMigrationHandler();
			return handler.migrateMicronodes(context)
				.doOnComplete(() -> {
					db().tx(() -> {
						JobWarningList warnings = new JobWarningList();
						setWarnings(warnings);
						finializeMigration(context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					db().tx(() -> {
						context.getStatus().error(err, "Error in micronode migration.");
						createBatch().add(createEvent(BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});
		});
	}

	private void finializeMigration(MicronodeMigrationContext context) {
		db().tx(() -> {
			createBatch().add(createEvent(MICROSCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
