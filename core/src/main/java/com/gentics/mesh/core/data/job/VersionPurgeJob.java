package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_VERSION_PURGE_FINISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.time.ZonedDateTime;
import java.util.Optional;

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

public interface VersionPurgeJob extends JobCore, HibVersionPurgeJob {

	static final Logger log = LoggerFactory.getLogger(VersionPurgeJob.class);

	@Override
	default Completable processTask(Database db) {
		ProjectVersionPurgeHandler handler = db.tx(tx -> { 
			return tx.<CommonTx>unwrap().data().mesh().projectVersionPurgeHandler(); 
		});
		HibProject project = db.tx(() -> getProject());
		Optional<ZonedDateTime> maxAge = db.tx(() -> getMaxAge());
		return handler.purgeVersions(project, maxAge.orElse(null))
			.doOnComplete(() -> {
				db.tx(() -> {
					setStopTimestamp();
					setStatus(COMPLETED);
				});
				db.tx(tx -> {
					log.info("Version purge job {" + getUuid() + "} for project {" + project.getName() + "} completed.");
					tx.createBatch().add(createEvent(PROJECT_VERSION_PURGE_FINISHED, COMPLETED, project.getName(), project.getUuid()))
						.dispatch();
				});
			}).doOnError(error -> {
				db.tx(() -> {
					setStopTimestamp();
					setStatus(FAILED);
					setError(error);
				});
				db.tx(tx -> {
					log.info("Version purge job {" + getUuid() + "} for project {" + project.getName() + "} failed.", error);
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
