package com.gentics.mesh.core.jobs;

import com.gentics.mesh.core.data.job.Job;
import io.reactivex.Completable;

/**
 * A job processor responsible for performing a single job
 */
public interface SingleJobProcessor {

	/**
	 * process a job
	 * @return completable
	 */
	Completable process(Job job);
}
