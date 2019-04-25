package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_VERSION_PURGE_FINISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.job.ProjectVersionPurgeEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VersionPurgeJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(VersionPurgeJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(VersionPurgeJobImpl.class, MeshVertexImpl.class);
	}

	public Project getProject() {
		return out(HAS_PROJECT).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	public void setProject(Project project) {
		setSingleLinkOutTo(project, HAS_PROJECT);
	}

	@Override
	protected Completable processTask() {
		Database db = DB.get();
		return Completable.complete()
			.doOnComplete(() -> {
				db.tx(() -> {
					setStopTimestamp();
					setStatus(COMPLETED);
				});
				db.tx(() -> {
					log.info("Version purge job " + getUuid() + " completed.");
					Project project = getProject();
					EventQueueBatch.create().add(createEvent(PROJECT_VERSION_PURGE_FINISHED, COMPLETED, project.getName(), project.getUuid()))
						.dispatch();
				});
			}).doOnError(error -> {
				db.tx(() -> {
					setStopTimestamp();
					setStatus(FAILED);
					setError(error);
				});
				db.tx(() -> {
					log.info("Version purge job " + getUuid() + " failed.", error);
					Project project = getProject();
					EventQueueBatch.create().add(createEvent(PROJECT_VERSION_PURGE_FINISHED, FAILED, project.getName(), project.getUuid()))
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
