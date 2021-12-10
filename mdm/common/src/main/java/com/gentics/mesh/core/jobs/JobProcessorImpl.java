package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class JobProcessorImpl implements JobProcessor {

	public static final Logger log = LoggerFactory.getLogger(JobProcessorImpl.class);

	final Map<JobType, SingleJobProcessor> jobProcessors;

	@Inject
	public JobProcessorImpl(Map<JobType, SingleJobProcessor> jobProcessors) {
		this.jobProcessors = jobProcessors;
	}

	@Override
	public Completable process() {
		List<Completable> actions = new ArrayList<>();
		Iterable<? extends HibJob> it = Tx.get().jobDao().findAll();
		for (HibJob job : it) {
			try {
				// Don't execute failed or completed jobs again
				JobStatus jobStatus = job.getStatus();
				if (job.hasFailed() || (jobStatus == COMPLETED || jobStatus == FAILED || jobStatus == UNKNOWN)) {
					continue;
				}
				actions.add(process(job));
			} catch (Exception e) {
				job.markAsFailed(e);
				log.error("Error while processing job {" + job.getUuid() + "}");
			}
		}
		return Completable.concat(actions);
	}

	private Completable process(HibJob job) {
		Database db = CommonTx.get().data().mesh().database();
		return Completable.defer(() -> {
			db.tx(() -> {
				log.info("Processing job {" + job.getUuid() + "}");
				job.setStartTimestamp();
				job.setStatus(STARTING);
				job.setNodeName();
			});

			return processTask(job, db);
		});
	}

	private CompletableSource processTask(HibJob job, Database db) {
		JobType jobType = db.tx((tx) -> {
			JobType type = job.getType();
			if (type == null) {
				throw error(BAD_REQUEST, "JobType with id {" + job.getUuid() + "} has null type.");
			}

			return type;
		});

		SingleJobProcessor jobProcessor = jobProcessors.get(jobType);
		if (jobProcessor == null) {
			throw new RuntimeException("Do not know how to process jobs of type {" + jobType + "}");
		}
		return jobProcessor.process(job);
	}
}
