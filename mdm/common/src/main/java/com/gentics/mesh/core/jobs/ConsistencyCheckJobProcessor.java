package com.gentics.mesh.core.jobs;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;

import io.reactivex.Completable;

/**
 * Job Processor for performing a consistency check
 */
@Singleton
public class ConsistencyCheckJobProcessor extends AbstractConsistencyCheckProcessor {
	/**
	 * Create instance
	 * @param db database
	 * @param handler consistency check handler
	 * @param jobDao job dao
	 */
	@Inject
	public ConsistencyCheckJobProcessor(Database db, ConsistencyCheckHandler handler, JobDao jobDao) {
		super(db, handler, jobDao);
	}

	@Override
	public Completable process(Job job) {
		return process(job, false);
	}
}
