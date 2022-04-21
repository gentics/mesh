package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.MicronodeMigrationContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobWarningList;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class is responsible for starting a micronode migration from a job
 */
public class MicronodeJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(MicronodeJobProcessor.class);
	private Database db;

	@Inject
	public MicronodeJobProcessor(Database db) {
		this.db = db;
	}

	/**
	 * Create a new migration event model object.
	 *
	 * @param job
	 * @param event
	 * @param status
	 * @return
	 */
	private MicroschemaMigrationMeshEventModel createEvent(HibJob job, Tx tx, MeshEvent event, JobStatus status) {
		MicroschemaMigrationMeshEventModel model = new MicroschemaMigrationMeshEventModel();
		model.setEvent(event);

		HibMicroschemaVersion toVersion = job.getToMicroschemaVersion();
		model.setToVersion(toVersion.transformToReference());

		HibMicroschemaVersion fromVersion = job.getFromMicroschemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		HibBranch branch = job.getBranch();
		if (branch != null) {
			HibProject project = branch.getProject();
			model.setProject(project.transformToReference());
			model.setBranch(branch.transformToReference());
		}

		model.setOrigin(tx.data().options().getNodeName());
		model.setStatus(status);
		return model;
	}

	private MicronodeMigrationContext prepareContext(HibJob job) {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(job.getUuid());
		try {
			return db.tx(tx -> {
				MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(job, tx, MICROSCHEMA_MIGRATION_START, STARTING)).dispatch();

				HibBranch branch = job.getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + job.getUuid() + "} not found");
				}
				context.setBranch(branch);

				HibMicroschemaVersion fromContainerVersion = job.getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + job.getUuid() + "} could not be found.");
				}
				context.setFromVersion(fromContainerVersion);

				HibMicroschemaVersion toContainerVersion = job.getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + job.getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				HibMicroschema schemaContainer = fromContainerVersion.getSchemaContainer();
				HibBranchMicroschemaVersion branchVersionEdge = Tx.get().branchDao().findBranchMicroschemaEdge(branch, toContainerVersion);
				context.getStatus().setVersionEdge(branchVersionEdge);
				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
							+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}

				MicroschemaMigrationCause cause = new MicroschemaMigrationCause();
				cause.setFromVersion(fromContainerVersion.transformToReference());
				cause.setToVersion(toContainerVersion.transformToReference());
				cause.setBranch(branch.transformToReference());
				cause.setOrigin(tx.data().options().getNodeName());
				cause.setUuid(job.getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db.tx(() -> {
				status.error(e, "Error while preparing micronode migration.");
			});
			throw e;
		}

	}

	@Override
	public Completable process(HibJob job) {
		MicronodeMigration handler = db.tx(tx -> {
			return tx.<CommonTx>unwrap().data().mesh().micronodeMigrationHandler();
		});
		return Completable.defer(() -> {
			MicronodeMigrationContext context = prepareContext(job);
			return handler.migrateMicronodes(context)
					.doOnComplete(() -> {
						db.tx(() -> {
							finalizeMigration(job, context);
							context.getStatus().done();
						});
					}).doOnError(err -> {
						db.tx(tx -> {
							context.getStatus().error(err, "Error in micronode migration.");
							tx.createBatch().add(createEvent(job, tx, BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
						});
					});
		});
	}

	private void finalizeMigration(HibJob job, MicronodeMigrationContext context) {
		db.tx(tx -> {
			tx.createBatch().add(createEvent(job, tx, MICROSCHEMA_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}
}
