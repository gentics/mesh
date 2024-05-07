package com.gentics.mesh.core.verticle.job;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.shareddata.Lock;

/**
 * Worker verticles are used to run job executions.
 */
public interface JobWorkerVerticle extends Verticle {

	/**
	 * Start the verticle.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Stop the verticle.
	 * 
	 * @throws Exception
	 */
	void stop() throws Exception;

	/**
	 * Wait for the global lock for the given timeout in ms and pass the result to the given handler
	 * @param timeout timeout in ms
	 * @param resultHandler result handler
	 */
	void doWithLock(long timeout, Handler<AsyncResult<Lock>> resultHandler);
}
