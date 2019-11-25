package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class MonitoringCrudHandler {
	private static final Logger log = LoggerFactory.getLogger(MonitoringCrudHandler.class);

	private final BootstrapInitializer boot;

	@Inject
	public MonitoringCrudHandler(BootstrapInitializer boot) {
		this.boot = boot;
	}

	public void handleLive(RoutingContext rc) {
		// We currently don't have a situation which would justify to let the service being restarted automatically.
		rc.response().setStatusCode(200).end();
	}

	public void handleReady(RoutingContext rc) {
		MeshStatus status = boot.mesh().getStatus();
		if (status.equals(MeshStatus.READY)) {
			rc.response().end();
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Status is {" + status.name() + "} - Failing readiness probe");
			}
			throw error(SERVICE_UNAVAILABLE, "error_internal");
		}
	}
}
