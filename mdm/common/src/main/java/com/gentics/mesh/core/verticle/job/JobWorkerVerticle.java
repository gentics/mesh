package com.gentics.mesh.core.verticle.job;

import io.vertx.core.Verticle;

public interface JobWorkerVerticle extends Verticle {

	void start() throws Exception;

	void stop() throws Exception;

}
