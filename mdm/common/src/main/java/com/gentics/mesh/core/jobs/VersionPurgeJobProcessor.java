package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_VERSION_PURGE_FINISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.HibVersionPurgeJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.job.ProjectVersionPurgeEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class is responsible for starting a version purge from a job
 */
public class VersionPurgeJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(VersionPurgeJobProcessor.class);
	private final Database db;
	private final PersistingJobDao jobDao;

	@Inject
	public VersionPurgeJobProcessor(Database db, JobDao jobDao) {
		this.db = db;
		this.jobDao = (PersistingJobDao) jobDao;
	}

	@Override
	public Completable process(HibJob job) {
		HibVersionPurgeJob purgeJob = (HibVersionPurgeJob) job;
		ProjectVersionPurgeHandler handler = db.tx(tx -> {
			return tx.<CommonTx>unwrap().data().mesh().projectVersionPurgeHandler();
		});
		HibProject project = db.tx(purgeJob::getProject);
		Optional<ZonedDateTime> maxAge = db.tx(purgeJob::getMaxAge);
		return handler.purgeVersions(project, maxAge.orElse(null))
				.doOnComplete(() -> {
					db.tx(() -> {
						purgeJob.setStopTimestamp();
						purgeJob.setStatus(COMPLETED);
						jobDao.mergeIntoPersisted(purgeJob);
					});
					db.tx(tx -> {
						log.info("Version purge job {" + purgeJob.getUuid() + "} for project {" + project.getName() + "} completed.");
						tx.createBatch().add(createEvent(PROJECT_VERSION_PURGE_FINISHED, COMPLETED, project.getName(), project.getUuid()))
								.dispatch();
					});
				}).doOnError(error -> {
					db.tx(() -> {
						purgeJob.setStopTimestamp();
						purgeJob.setStatus(FAILED);
						purgeJob.setError(error);
						jobDao.mergeIntoPersisted(purgeJob);
					});
					db.tx(tx -> {
						log.info("Version purge job {" + purgeJob.getUuid() + "} for project {" + project.getName() + "} failed.", error);
						tx.createBatch().add(createEvent(PROJECT_VERSION_PURGE_FINISHED, FAILED, project.getName(), project.getUuid()))
								.dispatch();
					});
				});
	}

	private ProjectVersionPurgeEventModel createEvent(MeshEvent event, JobStatus status, String name, String uuid) {
		ProjectVersionPurgeEventModel model = new ProjectVersionPurgeEventModel();
		model.setName(name);
		model.setUuid(uuid);
		model.setEvent(event);
		model.setStatus(status);
		return model;
	}
}
