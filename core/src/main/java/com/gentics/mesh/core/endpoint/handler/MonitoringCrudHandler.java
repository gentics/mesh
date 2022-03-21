package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for monitoring related actions.
 */
@Singleton
public class MonitoringCrudHandler {
	private static final Logger log = LoggerFactory.getLogger(MonitoringCrudHandler.class);

	private final BootstrapInitializer boot;

	private final MeshPluginManager pluginManager;

	private final ClusterManager clusterManager;

	private final LivenessManager liveness;

	private final LocalConfigApi localConfigApi;

	private final Database db;

	@Inject
	public MonitoringCrudHandler(BootstrapInitializer boot, MeshPluginManager pluginManager, ClusterManager clusterManager, LivenessManager liveness, LocalConfigApi localConfigApi, Database db) {
		this.boot = boot;
		this.pluginManager = pluginManager;
		this.clusterManager = clusterManager;
		this.liveness = liveness;
		this.localConfigApi = localConfigApi;
		this.db = db;
	}

	/**
	 * Check the cluster liveness probe.
	 * 
	 * @param rc
	 */
	public void handleLive(RoutingContext rc) {
		for (String id : pluginManager.getPluginIds()) {
			PluginStatus status = pluginManager.getStatus(id);
			if (status == PluginStatus.FAILED) {
				log.warn("Plugin {" + id + "} is in status failed.");
				throw error(SERVICE_UNAVAILABLE, "error_internal");
			}
		}

		if (!liveness.isLive()) {
			log.warn("Liveness was set to false due to {}", liveness.getError());
			throw error(SERVICE_UNAVAILABLE, "error_internal");
		}

		rc.response().setStatusCode(200).end();
	}

	/**
	 * Check the cluster readiness probe.
	 * 
	 * @param rc
	 */
	public void handleReady(RoutingContext rc) {
		for (String id : pluginManager.getPluginIds()) {
			PluginStatus status = pluginManager.getStatus(id);
			// TODO We need can't check for plugin registered since plugins will only be
			// registered once the write quorum has been reached.
			// Thus we can only check for failed. Otherwise we would interrupt the
			// K8S deployment process and prevent additional nodes from being added
			// to the cluster. Without additional nodes the write quorum would never
			// be reached.
			if (status == PluginStatus.FAILED) {
				log.error("Plugin {" + id + "} is in status failed.");
				throw error(SERVICE_UNAVAILABLE, "error_internal");
			}
		}

		if (!liveness.isLive()) {
			log.warn("Liveness was set to false due to {}", liveness.getError());
			throw error(SERVICE_UNAVAILABLE, "error_internal");
		}

		if (clusterManager != null && !clusterManager.isLocalNodeOnline()) {
			log.warn("Local node is not online - Failing readiness probe");
			throw error(SERVICE_UNAVAILABLE, "error_internal");
		}

		MeshStatus status = boot.mesh().getStatus();
		if (status.equals(MeshStatus.READY)) {
			rc.response().end();
		} else {
			log.warn("Status is {" + status.name() + "} - Failing readiness probe");
			throw error(SERVICE_UNAVAILABLE, "error_internal");
		}
	}

	/**
	 * Throw an error if:
	 * - mesh is in read only mode
	 * - topology lock is held
	 * - writeQuorum is not reached
	 *
	 * @param rc
	 */
	public void handleWritable(RoutingContext rc) {
		localConfigApi.getActiveConfig()
				.map(LocalConfigModel::isReadOnly)
				.map(Boolean::booleanValue)
				.subscribe(isReadOnly -> {
					if (isReadOnly) {
						log.warn("Local node cannot write - read only mode set");
						rc.fail(error(SERVICE_UNAVAILABLE, "error_internal"));
					} else if (db.isReadOnly(false)) {
						rc.fail(error(SERVICE_UNAVAILABLE, "error_internal"));
					} else if (clusterManager.isClusterTopologyLocked()) {
						log.warn("Local node cannot write - cluster topology locked");
						rc.fail(error(SERVICE_UNAVAILABLE, "error_internal"));
					} else if (!clusterManager.isWriteQuorumReached()) {
						log.warn("Local node cannot write - write quorum not reached");
						rc.fail(error(SERVICE_UNAVAILABLE, "error_internal"));
					} else {
						rc.response().setStatusCode(200).end();
					}
				});
	}
}
