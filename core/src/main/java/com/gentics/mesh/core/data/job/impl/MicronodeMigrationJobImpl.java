package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.MicronodeMigrationContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.endpoint.migration.micronode.MicronodeMigrationHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MicronodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	public MicroschemaMigrationMeshEventModel createEvent(MeshEvent event, JobStatus status) {
		MicroschemaMigrationMeshEventModel model = new MicroschemaMigrationMeshEventModel();
		model.setEvent(event);

		MicroschemaContainerVersion toVersion = getToMicroschemaVersion();
		model.setToVersion(toVersion.transformToReference());

		MicroschemaContainerVersion fromVersion = getFromMicroschemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		Branch branch = getBranch();
		Project project = branch.getProject();
		model.setProject(project.transformToReference());
		model.setBranch(branch.transformToReference());

		model.setOrigin(Mesh.mesh().getOptions().getNodeName());
		model.setStatus(status);
		return model;
	}

	private MicronodeMigrationContext prepareContext() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), JobType.microschema);
		try {
			return DB.get().tx(() -> {
				MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
				context.setStatus(status);

				EventQueueBatch.create().add(createEvent(MICROSCHEMA_MIGRATION_START, STARTING)).dispatch();

				Branch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}
				context.setBranch(branch);

				MicroschemaContainerVersion fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				MicroschemaContainerVersion toContainerVersion = getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				MicroschemaContainer schemaContainer = fromContainerVersion.getSchemaContainer();
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
				cause.setOrigin(Mesh.mesh().getOptions().getNodeName());
				cause.setUuid(getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			DB.get().tx(() -> {
				status.error(e, "Error while preparing micronode migration.");
			});
			throw e;
		}

	}

	protected Completable processTask() {
		return Completable.defer(() -> {
			MicronodeMigrationContext context = prepareContext();
			MicronodeMigrationHandler handler = MeshInternal.get().micronodeMigrationHandler();
			return handler.migrateMicronodes(context)
				.doOnComplete(() -> {
					DB.get().tx(() -> {
						JobWarningList warnings = new JobWarningList();
						setWarnings(warnings);
						finializeMigration(context);
						context.getStatus().done();
					});
				}).doOnError(err -> {
					DB.get().tx(() -> {
						context.getStatus().error(err, "Error in micronode migration.");
						EventQueueBatch.create().add(createEvent(BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
					});
				});
		});
	}

	private void finializeMigration(MicronodeMigrationContext context) {
		DB.get().tx(() -> {
			EventQueueBatch.create().add(createEvent(MICROSCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}

}
