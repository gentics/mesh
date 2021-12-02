package com.gentics.mesh.core.jobs;

import io.reactivex.Completable;

/**
 * A class responsible for processing all jobs
 */
public interface JobProcessor {

	/**
	 * process all jobs
	 * @return completable
	 */
	Completable process();
}
