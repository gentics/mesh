package com.gentics.mesh.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Various wrappers for verticle deployment calls.
 */
public final class DeploymentUtil {

	public static final Logger log = LoggerFactory.getLogger(DeploymentUtil.class);

	// TODO decrease
	private static final long DEFAULT_TIMEOUT_IN_SECONDS = 10 * 1000;

	/**
	 * Deploy the given verticle.
	 * 
	 * @param vertx
	 *            Vertex instance which should be deployed into
	 * @param config
	 *            Verticle configuration
	 * @param verticle
	 *            Verticle which should be deployed
	 * @param worker
	 *            Flag which indicates whether the verticle should be deployed as worker verticle
	 * @return Deployment Id
	 * @throws Exception
	 */
	public static String deployAndWait(Vertx vertx, JsonObject config, AbstractVerticle verticle, boolean worker)
			throws Exception {
		CompletableFuture<String> fut = new CompletableFuture<>();
		DeploymentOptions options = new DeploymentOptions();
		if (config != null) {
			options = new DeploymentOptions(new JsonObject().put("config", config));
		}
		options.setThreadingModel(worker ? ThreadingModel.WORKER : DeploymentOptions.DEFAULT_MODE);
		vertx.deployVerticle(verticle, options).onComplete(deploymentId -> {
			if (log.isDebugEnabled()) {
				log.debug("Deployed verticle {" + verticle.getClass().getName() + "} => " + deploymentId);
			}
			fut.complete(deploymentId);
		}, error -> {
			log.error("Error:", error);
			fut.completeExceptionally(error);
		});
		return fut.get(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
	}

}
