package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.rest.job.JobStatus;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class is responsible for starting a branch migration from a job
 */
public class BranchJobProcessor implements SingleJobProcessor {
	public static final Logger log = LoggerFactory.getLogger(BranchJobProcessor.class);

	private final Database db;
	private final JobDao jobDao;

	@Inject
	public BranchJobProcessor(Database db, JobDao jobDao) {
		this.db = db;
		this.jobDao = jobDao;
	}

	/**
	 * Create a new branch migration event.
	 *
	 * @param event
	 * @param status
	 * @return
	 */
	private BranchMigrationMeshEventModel createEvent(HibJob job, MeshEvent event, JobStatus status) {
		BranchMigrationMeshEventModel model = new BranchMigrationMeshEventModel();
		model.setEvent(event);

		HibBranch newBranch = job.getBranch();
		model.setBranch(newBranch.transformToReference());

		HibProject project = newBranch.getProject();
		model.setProject(project.transformToReference());

		model.setStatus(status);
		model.setOrigin(Tx.get().data().options().getNodeName());
		return model;
	}

	private BranchMigrationContext prepareContext(HibJob job) {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(job.getUuid());
		log.debug("Preparing branch migration job");
		try {
			return db.tx(tx -> {
				BranchMigrationContextImpl context = new BranchMigrationContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(job, BRANCH_MIGRATION_START, STARTING)).dispatch();

				HibBranch newBranch = job.getBranch();
				if (newBranch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + job.getUuid() + "} cannot be found.");
				}
				if (newBranch.isMigrated()) {
					throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} is already migrated");
				}
				context.setNewBranch(newBranch);

				HibBranch oldBranch = newBranch.getPreviousBranch();
				if (oldBranch == null) {
					throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} does not have previous branch");
				}
				if (!oldBranch.isMigrated()) {
					throw error(BAD_REQUEST, "Cannot migrate nodes to branch {" + newBranch.getName() + "}, because previous branch {"
							+ oldBranch.getName() + "} is not fully migrated yet.");
				}
				context.setOldBranch(oldBranch);

				BranchMigrationCause cause = new BranchMigrationCause();
				cause.setProject(newBranch.getProject().transformToReference());
				cause.setOrigin(tx.data().options().getNodeName());
				cause.setUuid(job.getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db.tx(() -> {
				status.error(e, "Error while preparing branch migration.");
			});
			throw e;
		}
	}

	@Override
	public Completable process(HibJob job) {
		BranchMigration handler = db.tx(tx -> {
			return tx.<CommonTx>unwrap().data().mesh().branchMigrationHandler();
		});
		return Completable.defer(() -> {
			BranchMigrationContext context = prepareContext(job);
			return handler.migrateBranch(context)
					.doOnComplete(() -> {
						db.tx(() -> {
							// Job is nullable in the case of some unit tests
							if (job != null) {
								HibJob latest = jobDao.findByUuid(job.getUuid());
								finalizeMigration(latest, context);
							}
							context.getStatus().done();
						});
					}).doOnError(err -> {
						db.tx(tx -> {
							// Job is nullable in the case of some unit tests
							if (job != null) {
								HibJob latest = jobDao.findByUuid(job.getUuid());
								tx.createBatch().add(createEvent(latest, BRANCH_MIGRATION_FINISHED, FAILED)).dispatch();
							}
							context.getStatus().error(err, "Error in branch migration.");							
						});
					});
		});
	}

	private void finalizeMigration(HibJob job, BranchMigrationContext context) {
		// Mark branch as active & migrated
		db.tx(() -> {
			PersistingBranchDao persistingBranchDao = CommonTx.get().branchDao();
			HibBranch branch = job.getBranch();
			branch.setActive(true);
			branch.setMigrated(true);
			persistingBranchDao.mergeIntoPersisted(branch.getProject(), branch);
		});
		db.tx(tx -> {
			tx.createBatch().add(createEvent(job, BRANCH_MIGRATION_FINISHED, COMPLETED)).dispatch();
		});
	}
}
