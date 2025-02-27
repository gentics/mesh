package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.warning.JobWarning;
import com.google.common.base.Throwables;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Abstract base class for consistency check processors
 */
public abstract class AbstractConsistencyCheckProcessor implements SingleJobProcessor {
	private final Database db;

	private final ConsistencyCheckHandler handler;

	private final PersistingJobDao jobDao;

	/**
	 * Create instance
	 * @param db database
	 * @param handler consistency check handler
	 * @param jobDao job dao
	 */
	public AbstractConsistencyCheckProcessor(Database db, ConsistencyCheckHandler handler, JobDao jobDao) {
		this.db = db;
		this.handler = handler;
		this.jobDao = (PersistingJobDao) jobDao;
	}

	/**
	 * Process the given job (which is supposed to be a concistency check/repair job)
	 * @param job job
	 * @param attemptRepair true to repair, false to check only
	 * @return completable
	 */
	protected Completable process(HibJob job, boolean attemptRepair) {
		return Single.defer(() -> {
			ConsistencyCheckResponse result = handler.checkConsistency(attemptRepair, true).runInNewTx();
			return Single.just(result);
		}).flatMapCompletable(result -> {
			db.tx(tx -> {
				List<JobWarning> warnings = result.getInconsistencies().stream().map(inconsistency -> {
					JobWarning warning = new JobWarning();
					warning.setType("inconsistency");
					warning.setMessage(inconsistency.getDescription());
					Map<String, String> props = warning.getProperties();
					props.put("severity", inconsistency.getSeverity().name());
					props.put("repairAction", inconsistency.getRepairAction().name());
					if (inconsistency.isRepaired()) {
						props.put("repaired", "true");
					}
					return warning;
				}).collect(Collectors.toList());
				if (!warnings.isEmpty()) {
					JobWarningList jobWarningList = new JobWarningList();
					jobWarningList.setWarnings(warnings);
					job.setWarnings(jobWarningList);
				}
				job.setStopTimestamp();
				job.setStatus(COMPLETED);
				jobDao.mergeIntoPersisted(job);
			});
			return Completable.complete();
		}).doOnError(error -> {
			db.tx(() -> {
				job.setStopTimestamp();
				job.setStatus(FAILED);
				job.setError(Throwables.getRootCause(error));
				jobDao.mergeIntoPersisted(job);
			});
		});
	}
}
