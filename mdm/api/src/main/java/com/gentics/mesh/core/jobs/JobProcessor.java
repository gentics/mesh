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

	/**
	 * Check whether jobs are currently being processed
	 * @return true when jobs are processed, false if not
	 */
	boolean isProcessing();
}
