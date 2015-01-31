package com.gentics.cailun.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.cli.CaiLun;

public final class DeploymentUtils {

	private static final Logger log = LoggerFactory.getLogger(CaiLun.class);
	
	public static String deployAndWait(Vertx vertx, final Class<? extends AbstractVerticle> clazz) throws InterruptedException {
		return deployAndWait(vertx, clazz.getCanonicalName());
	}

	public static String deployAndWait(Vertx vertx, String verticleClass) throws InterruptedException {
		String prefix = SpringVerticleFactory.PREFIX + ":";
		return deployAndWait(vertx, prefix, verticleClass);
	}

	public static String deployAndWait(Vertx vertx, String prefix, String verticleClass) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> deploymentId = new AtomicReference<String>();
		vertx.deployVerticle(prefix + verticleClass, handler -> {
			if (handler.succeeded()) {
				deploymentId.set(handler.result());
				log.info("Deployed verticle {" + verticleClass + "} => " + deploymentId);
			} else {
				log.info("Error:", handler.cause());
			}
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
		return deploymentId.get();
	}

}
