package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.util.VersionUtil;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for fetching all jobs and running them
 */
@Singleton
public class JobProcessorImpl implements JobProcessor {

	public static final Logger log = LoggerFactory.getLogger(JobProcessorImpl.class);

	final Map<JobType, SingleJobProcessor> jobProcessors;
	private Database db;

	private boolean processing = false;

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

		return Completable.concat(jobsActions).doOnSubscribe(d -> {
			processing = true;
		}).doAfterTerminate(() -> {
			processing = false;
		});
	}

	@Override
	public boolean isProcessing() {
		return processing;
	}

	private List<? extends HibJob> getJobsToExecute() {
		return db.tx((tx) -> {
			return orderMigrationChains(tx.jobDao().findAll().stream()
					// Don't execute failed or completed jobs again
					.filter(job -> !(job.hasFailed() || (job.getStatus() == COMPLETED || job.getStatus() == FAILED || job.getStatus() == UNKNOWN)))
					.collect(Collectors.toList()));
		});
	}

	/**
	 * Migration jobs for the same (micro)schema and branch build on each other (e.g. 3.0 -> 4.0 and 4.0 -> 5.0) and must be executed in version
	 * order, but the database returns the jobs in no defined order. Reorder those jobs by their source version, keeping all other jobs at their
	 * positions.
	 *
	 * @param jobs jobs to execute
	 * @return jobs in execution order
	 */
	private static List<HibJob> orderMigrationChains(List<? extends HibJob> jobs) {
		List<HibJob> ordered = new ArrayList<>(jobs);
		Map<String, List<Integer>> positionsPerChain = new LinkedHashMap<>();
		for (int i = 0; i < ordered.size(); i++) {
			String chainKey = migrationChainKey(ordered.get(i));
			if (chainKey != null) {
				positionsPerChain.computeIfAbsent(chainKey, k -> new ArrayList<>()).add(i);
			}
		}

		for (List<Integer> positions : positionsPerChain.values()) {
			if (positions.size() < 2) {
				continue;
			}
			List<HibJob> chain = positions.stream().map(ordered::get).sorted(
					Comparator.comparing(JobProcessorImpl::fromVersion, VersionUtil::compareVersions)).collect(Collectors.toList());
			for (int i = 0; i < positions.size(); i++) {
				ordered.set(positions.get(i), chain.get(i));
			}
		}
		return ordered;
	}

	/**
	 * Return the key of the migration chain the job belongs to, or null if the job is not a (micro)schema migration with source version and branch.
	 */
	private static String migrationChainKey(HibJob job) {
		if (job.getBranch() == null || fromVersion(job) == null) {
			return null;
		}
		if (job.getType() == JobType.schema) {
			return "schema:" + job.getFromSchemaVersion().getSchemaContainer().getUuid() + ":" + job.getBranch().getUuid();
		} else {
			return "microschema:" + job.getFromMicroschemaVersion().getSchemaContainer().getUuid() + ":" + job.getBranch().getUuid();
		}
	}

	/**
	 * Return the source version of a (micro)schema migration job, or null for other jobs or if the version is missing.
	 */
	private static String fromVersion(HibJob job) {
		if (job.getType() == JobType.schema && job.getFromSchemaVersion() != null) {
			return job.getFromSchemaVersion().getVersion();
		} else if (job.getType() == JobType.microschema && job.getFromMicroschemaVersion() != null) {
			return job.getFromMicroschemaVersion().getVersion();
		}
		return null;
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
