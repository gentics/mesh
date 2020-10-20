package com.gentics.mesh.core.endpoint.admin;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ShutdownHandler {
	private static final Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

	private final BootstrapInitializer boot;

	@Inject
	public ShutdownHandler(BootstrapInitializer boot) {
		this.boot = boot;
	}

	public void shutdown(InternalActionContext context) {
		log.info("Initiating shutdown");
		context.send(new GenericMessageResponse("Shutdown initiated"), HttpResponseStatus.OK);
		Completable.fromAction(() -> {
			boot.mesh().shutdownAndTerminate(1);
		})
		.subscribeOn(Schedulers.newThread()).timeout(1, TimeUnit.MINUTES)
		.subscribe(() -> log.info("Shutdown successful"), err -> {
			log.error("Shutdown failed", err);
			log.error("Forcing process exit");
			Runtime.getRuntime().halt(1);
		});
	}
}
