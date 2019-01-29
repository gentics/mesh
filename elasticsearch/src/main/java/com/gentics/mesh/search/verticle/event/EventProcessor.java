package com.gentics.mesh.search.verticle.event;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;

public interface EventProcessor {

	/**
	 * Process this batch by invoking process on all batch entries.
	 * 
	 * @return
	 */
	Completable processAsync();

	/**
	 * Process this batch blocking and fail if the given timeout was exceeded.
	 * 
	 * @param timeout
	 * @param unit
	 */
	void processSync(long timeout, TimeUnit unit);

	/**
	 * Process this batch and block until it finishes. Apply a default timeout on
	 * this operation.
	 */
	void processSync();

}
