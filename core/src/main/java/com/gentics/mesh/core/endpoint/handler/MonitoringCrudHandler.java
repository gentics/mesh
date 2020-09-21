package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class MonitoringCrudHandler {
	private static final Logger log = LoggerFactory.getLogger(MonitoringCrudHandler.class);

	private final BootstrapInitializer boot;

	private final MeshPluginManager pluginManager;

	@Inject
	public MonitoringCrudHandler(BootstrapInitializer boot, MeshPluginManager pluginManager) {
		this.boot = boot;
		this.pluginManager = pluginManager;
	}

	public void handleLive(RoutingContext rc) {
		for (String id : pluginManager.getPluginIds()) {
			PluginStatus status = pluginManager.getStatus(id);
			if (status == PluginStatus.FAILED) {
				if (log.isDebugEnabled()) {
					log.debug("Plugin {" + id + "} is in status failed.");
				}
				throw error(SERVICE_UNAVAILABLE, "error_internal");
			}
		}
		rc.response().setStatusCode(200).end();
	}

	public void handleReady(RoutingContext rc) {
		for (String id : pluginManager.getPluginIds()) {
			PluginStatus status = pluginManager.getStatus(id);
			if (status != PluginStatus.REGISTERED) {
				if (log.isDebugEnabled()) {
					log.debug("Plugin {" + id + "} is not ready. Got status {" + status + "}");
				}
				throw error(SERVICE_UNAVAILABLE, "error_internal");
			}
		}

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
