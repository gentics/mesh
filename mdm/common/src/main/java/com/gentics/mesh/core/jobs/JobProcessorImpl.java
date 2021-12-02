package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JobProcessorImpl implements JobProcessor {

	public static final Logger log = LoggerFactory.getLogger(JobProcessorImpl.class);

	// TODO: inject in constructor
	ImmutableMap<JobType, SingleJobProcessor> jobProcessors = new ImmutableMap.Builder<JobType, SingleJobProcessor>()
			.put(JobType.schema, null)
			.put(JobType.microschema, null)
			.put(JobType.branch, null)
			.put(JobType.versionpurge, null)
			.build();

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

			return processTask(job);
		});
	}

	private CompletableSource processTask(HibJob job) {
		SingleJobProcessor jobProcessor = jobProcessors.get(job.getType());
		if (jobProcessor == null) {
			throw new RuntimeException("Do not know how to process jobs of type {" + job.getType() + "}");
		}
		return jobProcessor.process(job);
	}
}
