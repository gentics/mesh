package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobType;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class is responsible for fetching all jobs and running them
 */
@Singleton
public class JobProcessorImpl implements JobProcessor {

	public static final Logger log = LoggerFactory.getLogger(JobProcessorImpl.class);

	final Map<JobType, SingleJobProcessor> jobProcessors;
	private Database db;

	@Inject
	public JobProcessorImpl(Map<JobType, SingleJobProcessor> jobProcessors, Database db) {
		this.jobProcessors = jobProcessors;
		this.db = db;
	}

	@Override
	public Completable process() {
		List<Completable> jobsActions = getJobsToExecute().stream()
				.map(this::process)
				.collect(Collectors.toList());

		return Completable.concat(jobsActions);
	}

	private List<? extends HibJob> getJobsToExecute() {
		return db.tx((tx) -> {
			return Tx.get().jobDao().findAll().stream()
					// Don't execute failed or completed jobs again
					.filter(job -> !(job.hasFailed() || (job.getStatus() == COMPLETED || job.getStatus() == FAILED || job.getStatus() == UNKNOWN)))
					.collect(Collectors.toList());
		});
	}

	private Completable process(HibJob job) {
		return Completable.defer(() -> {
			HibJob startedJob = db.tx(() -> {
				log.info("Processing job {" + job.getUuid() + "}");
				job.setStartTimestamp();
				job.setStatus(STARTING);
				job.setNodeName();
				return CommonTx.get().jobDao().mergeIntoPersisted(job);
			});

			return processTask(startedJob);
		});
	}

	private CompletableSource processTask(HibJob job) {
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
