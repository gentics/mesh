package com.gentics.cailun.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.cli.CaiLun;

/**
 * Various wrappers for verticle deployment calls.
 * 
 * @author johannes2
 *
 */
public final class DeploymentUtils {

	private static final Logger LOG = LoggerFactory.getLogger(CaiLun.class);

	// TODO decrease
	private static final long DEFAULT_TIMEOUT_IN_SECONDS = 10 * 1000;

	public static String deployAndWait(Vertx vertx, JsonObject config, final Class<? extends AbstractVerticle> clazz) throws InterruptedException {
		return deployAndWait(vertx, config, clazz.getCanonicalName());
	}

	public static String deployAndWait(Vertx vertx, JsonObject config, String verticleClass) throws InterruptedException {
		String prefix = SpringVerticleFactory.PREFIX + ":";
		return deployAndWait(vertx, config, prefix, verticleClass);
	}

	public static String deployAndWait(Vertx vertx, JsonObject config, String prefix, String verticleClass) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> deploymentId = new AtomicReference<String>();
		DeploymentOptions options = new DeploymentOptions();
		if (config != null) {
			options = new DeploymentOptions(new JsonObject().put("config", config));
		}
		vertx.deployVerticle(prefix + verticleClass, options, handler -> {
			if (handler.succeeded()) {
				deploymentId.set(handler.result());
				if (LOG.isInfoEnabled()) {
					LOG.info("Deployed verticle {" + verticleClass + "} => " + deploymentId);
				}
			} else {
				if (LOG.isInfoEnabled()) {
					LOG.info("Error:", handler.cause());
				}
			}
			latch.countDown();
		});
		if (latch.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
			return deploymentId.get();
		} else {
			throw new InterruptedException("Timeout for startup reached");
		}
	}

}
