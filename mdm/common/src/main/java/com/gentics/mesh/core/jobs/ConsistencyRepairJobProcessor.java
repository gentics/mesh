package com.gentics.mesh.core.jobs;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;

import io.reactivex.Completable;

/**
 * Job Processor for performing a consistency repair
 */
@Singleton
public class ConsistencyRepairJobProcessor extends AbstractConsistencyCheckProcessor {
	/**
	 * Create instance
	 * @param db database
	 * @param handler consistency check handler
	 * @param jobDao job dao
	 */
	@Inject
	public ConsistencyRepairJobProcessor(Database db, ConsistencyCheckHandler handler, JobDao jobDao) {
		super(db, handler, jobDao);
	}

	@Override
	public Completable process(HibJob job) {
		return process(job, true);
	}
}
