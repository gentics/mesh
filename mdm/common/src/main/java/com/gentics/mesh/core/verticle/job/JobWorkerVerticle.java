package com.gentics.mesh.core.verticle.job;

import io.vertx.core.Verticle;

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

}
